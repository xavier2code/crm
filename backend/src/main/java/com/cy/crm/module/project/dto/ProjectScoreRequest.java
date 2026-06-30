package com.cy.crm.module.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "项目双精评分请求")
public class ProjectScoreRequest {

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "项目ID")
    private Long projectId;

    @NotEmpty(message = "评分项不能为空")
    @Schema(description = "评分项列表")
    private List<ScoreItem> scores;

    @Data
    @Schema(description = "评分项")
    public static class ScoreItem {
        @Schema(description = "维度编码")
        private String dimension;

        @Schema(description = "分数 0-100")
        private BigDecimal score;
    }
}
