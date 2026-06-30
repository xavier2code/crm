package com.cy.crm.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "工作台响应")
public class DashboardVO {

    @Schema(description = "渠道ID")
    private Long channelId;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "总客户数")
    private Integer totalCustomers;

    @Schema(description = "总商机数")
    private Integer totalOpportunities;

    @Schema(description = "生效中商机数")
    private Integer activeOpportunities;

    @Schema(description = "总项目数")
    private Integer totalProjects;

    @Schema(description = "项目中数量")
    private Integer inProgressProjects;

    @Schema(description = "总合同金额（万元）")
    private BigDecimal totalContractAmount;

    @Schema(description = "业绩完成率")
    private BigDecimal performanceRate;

    @Schema(description = "待办任务数")
    private Integer pendingTasks;

    @Schema(description = "今日待跟进客户数")
    private Integer todayFollowUpCount;

    @Schema(description = "近期跟进记录")
    private List<FollowUpSummaryVO> recentFollowUps;

    @Schema(description = "即将到期的项目")
    private List<ProjectExpiringVO> expiringProjects;

    @Schema(description = "商机状态分布")
    private Map<String, Integer> opportunityStatusDistribution;

    @Schema(description = "项目阶段分布")
    private Map<String, Integer> projectStageDistribution;

    @Data
    @Schema(description = "跟进记录摘要")
    public static class FollowUpSummaryVO {
        @Schema(description = "客户名称")
        private String customerName;

        @Schema(description = "跟进日期")
        private String followUpDate;

        @Schema(description = "跟进内容摘要")
        private String contentSummary;
    }

    @Data
    @Schema(description = "即将到期项目")
    public static class ProjectExpiringVO {
        @Schema(description = "项目名称")
        private String projectName;

        @Schema(description = "客户名称")
        private String customerName;

        @Schema(description = "到期日期")
        private String expireDate;

        @Schema(description = "剩余天数")
        private Integer remainingDays;
    }
}
