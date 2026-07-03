package com.cy.crm.module.rebate.scheduler;

import com.cy.crm.module.rebate.service.RebateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RebateGenerationScheduler {

    private final RebateService rebateService;

    /**
     * 每日凌晨 2 点执行：
     * - 补录已签合同但未生成业绩完成返利的记录
     * - 为已到账回款节点生成回款返利
     * - 为服务期满 9 个月的项目生成服务返利
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateRebates() {
        log.info("开始执行返利自动生成任务...");

        LocalDate today = LocalDate.now();

        try {
            int performanceCount = rebateService.generateMissingPerformanceRebates();
            log.info("业绩完成返利生成完成，新增 {} 条", performanceCount);

            int paymentCount = rebateService.generatePaymentRebates();
            log.info("回款返利生成完成，新增 {} 条", paymentCount);

            int serviceCount = rebateService.generateServiceRebates(today);
            log.info("服务返利生成完成，新增 {} 条", serviceCount);
        } catch (Exception e) {
            log.error("返利自动生成任务执行失败", e);
        }

        log.info("返利自动生成任务结束");
    }

    /**
     * 每月 1 号凌晨 1 点执行：按服务期进度更新业绩完成返利的实发金额。
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    public void updateActualPerformanceAmounts() {
        log.info("开始更新业绩完成返利实发金额...");

        try {
            int updated = rebateService.updateActualPerformanceAmounts(LocalDate.now());
            log.info("业绩完成返利实发金额更新完成，更新 {} 条", updated);
        } catch (Exception e) {
            log.error("更新业绩完成返利实发金额失败", e);
        }
    }
}
