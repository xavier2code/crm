package com.cy.crm.module.notification.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.crm.module.notification.service.NotificationService;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.opportunity.service.OpportunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpportunityExpiryScheduler {

    private final OpportunityMapper opportunityMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void checkOpportunityExpiry() {
        log.info("开始检查报备失效...");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Opportunity> activeOpportunities = opportunityMapper.selectList(
                new QueryWrapper<Opportunity>()
                        .eq("status", OpportunityService.STATUS_ACTIVE)
                        .le("last_follow_up_at", thirtyDaysAgo)
        );

        for (Opportunity opp : activeOpportunities) {
            if (shouldExpire(opp)) {
                expireOpportunity(opp);
            } else {
                sendWarningNotification(opp);
            }
        }

        log.info("报备失效检查完成，处理了 {} 条记录", activeOpportunities.size());
    }

    private boolean shouldExpire(Opportunity opp) {
        LocalDateTime lastFollowUp = opp.getLastFollowUpAt();
        if (lastFollowUp == null) {
            LocalDateTime effectiveAt = opp.getEffectiveAt();
            return effectiveAt != null && effectiveAt.plusDays(30).isBefore(LocalDateTime.now());
        }
        return lastFollowUp.plusDays(30).isBefore(LocalDateTime.now());
    }

    private void expireOpportunity(Opportunity opp) {
        log.info("报备 {} 超过30天未跟进，设置为失效", opp.getId());

        opp.setStatus(OpportunityService.STATUS_EXPIRED);
        opp.setExpiredAt(LocalDateTime.now());

        if (opp.getSubmitCount() != null && opp.getSubmitCount() >= 2) {
            opp.setCoolingUntil(LocalDateTime.now().plusMonths(1));
        }

        opportunityMapper.updateById(opp);

        notificationService.createNotification(
                opp.getSubmittedBy(),
                "报备已失效",
                String.format("您的报备 %s 已超过30天未跟进，现已失效。", opp.getId()),
                "OPPORTUNITY_EXPIRED",
                opp.getId()
        );
    }

    private void sendWarningNotification(Opportunity opp) {
        LocalDateTime lastFollowUp = opp.getLastFollowUpAt();
        if (lastFollowUp != null) {
            long daysSinceLastFollow = java.time.temporal.ChronoUnit.DAYS.between(lastFollowUp, LocalDateTime.now());
            if (daysSinceLastFollow >= 25 && daysSinceLastFollow <= 30) {
                notificationService.createNotification(
                        opp.getSubmittedBy(),
                        "报备即将失效提醒",
                        String.format("您的报备 %s 已%d天未跟进，请在%d天内进行跟进以避免失效。", opp.getId(), daysSinceLastFollow, 30 - daysSinceLastFollow),
                        "OPPORTUNITY_WARNING",
                        opp.getId()
                );
            }
        }
    }
}
