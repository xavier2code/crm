package com.cy.crm.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "项目统计响应")
public class ProjectStatisticsVO {

    @Schema(description = "总项目数")
    private Integer totalProjects;

    @Schema(description = "P级节点分布")
    private Map<String, Integer> pNodeDistribution;

    @Schema(description = "6大阶段分布")
    private Map<String, Integer> stage6Distribution;

    @Schema(description = "客户分层分布")
    private Map<String, Integer> customerLayerDistribution;

    @Schema(description = "平均双精评分")
    private BigDecimal avgScore;

    @Schema(description = "高评分项目数（>=80分）")
    private Integer highScoreCount;

    @Schema(description = "中评分项目数（60-79分）")
    private Integer mediumScoreCount;

    @Schema(description = "低评分项目数（<60分）")
    private Integer lowScoreCount;

    @Schema(description = "里程碑完成率统计")
    private List<MilestoneStatisticsVO> milestoneStatistics;

    @Data
    @Schema(description = "里程碑统计")
    public static class MilestoneStatisticsVO {
        @Schema(description = "里程碑名称")
        private String name;

        @Schema(description = "完成数量")
        private Integer completedCount;

        @Schema(description = "完成率")
        private BigDecimal rate;
    }
}
