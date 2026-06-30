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
public class OpportunityApprovalReminderScheduler {

    private final OpportunityMapper opportunityMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * ?")
    public void checkApprovalTimeout() {
        log.info("开始检查报备审批超时...");

        LocalDateTime fortyEightHoursAgo = LocalDateTime.now().minusHours(48);

        List<Opportunity> pendingOpportunities = opportunityMapper.selectList(
                new QueryWrapper<Opportunity>()
                        .eq("status", OpportunityService.STATUS_PENDING)
                        .le("created_at", fortyEightHoursAgo)
        );

        for (Opportunity opp : pendingOpportunities) {
            notifyCYBDForApproval(opp);
        }

        log.info("审批超时检查完成，发现 {} 条超时记录", pendingOpportunities.size());
    }

    private void notifyCYBDForApproval(Opportunity opp) {
        long hoursPending = java.time.temporal.ChronoUnit.HOURS.between(
                opp.getCreatedAt(),
                LocalDateTime.now()
        );

        notificationService.createNotification(
                getCYBDUserId(),
                "报备审批超时提醒",
                String.format("报备 %s 已提交%d小时未审批，请及时处理。", opp.getId(), hoursPending),
                "APPROVAL_TIMEOUT",
                opp.getId()
        );
    }

    private Long getCYBDUserId() {
        return 1L;
    }
}
