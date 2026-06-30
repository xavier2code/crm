package com.cy.crm.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "渠道工作台响应")
public class ChannelDashboardVO {

    @Schema(description = "渠道ID")
    private Long channelId;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "渠道下成员数")
    private Integer memberCount;

    @Schema(description = "总客户数")
    private Integer totalCustomers;

    @Schema(description = "总商机数")
    private Integer totalOpportunities;

    @Schema(description = "生效中商机数")
    private Integer activeOpportunities;

    @Schema(description = "失效商机数")
    private Integer expiredOpportunities;

    @Schema(description = "总项目数")
    private Integer totalProjects;

    @Schema(description = "项目中数量")
    private Integer inProgressProjects;

    @Schema(description = "已完成项目数")
    private Integer completedProjects;

    @Schema(description = "总合同金额（万元）")
    private BigDecimal totalContractAmount;

    @Schema(description = "本年度合同金额（万元）")
    private BigDecimal yearContractAmount;

    @Schema(description = "应发返利总额")
    private BigDecimal totalRebateAmount;

    @Schema(description = "实发返利总额")
    private BigDecimal actualRebateAmount;

    @Schema(description = "未确认返利金额")
    private BigDecimal unconfirmedRebateAmount;

    @Schema(description = "成员业绩分布")
    private List<MemberPerformanceVO> memberPerformances;

    @Schema(description = "客户分布按区域")
    private Map<String, Integer> customerRegionDistribution;

    @Schema(description = "项目状态分布")
    private Map<String, Integer> projectStatusDistribution;

    @Schema(description = "产品类别分布")
    private Map<String, Integer> productCategoryDistribution;

    @Data
    @Schema(description = "成员业绩")
    public static class MemberPerformanceVO {
        @Schema(description = "成员ID")
        private Long userId;

        @Schema(description = "成员姓名")
        private String userName;

        @Schema(description = "客户数")
        private Integer customerCount;

        @Schema(description = "商机数")
        private Integer opportunityCount;

        @Schema(description = "项目数")
        private Integer projectCount;

        @Schema(description = "合同金额（万元）")
        private BigDecimal contractAmount;
    }
}
