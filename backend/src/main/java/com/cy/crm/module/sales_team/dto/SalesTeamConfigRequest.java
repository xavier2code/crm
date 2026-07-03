package com.cy.crm.module.sales_team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 销售梯队配置请求 DTO
 */
@Data
@Schema(description = "销售梯队配置请求")
public class SalesTeamConfigRequest {

    @NotBlank(message = "梯队编码不能为空")
    @Size(max = 32, message = "梯队编码不能超过 32 字符")
    @Schema(description = "梯队编码，如 1 / 2 / 3 / 4 / XUQIAN")
    private String teamCode;

    @NotBlank(message = "梯队名称不能为空")
    @Size(max = 64, message = "梯队名称不能超过 64 字符")
    @Schema(description = "梯队名称，如 第一梯队 / 续签组")
    private String teamName;

    @NotBlank(message = "区域编码不能为空")
    @Size(max = 64, message = "区域编码不能超过 64 字符")
    @Schema(description = "区域字典 code")
    private String regionCode;

    @Size(max = 2000, message = "单位列表不能超过 2000 字符")
    @Schema(description = "包含的单位 code，逗号分隔")
    private String unitCodes;

    @Schema(description = "排序")
    private Integer sort;

    @Size(max = 255, message = "备注不能超过 255 字符")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "有效期起")
    private LocalDate effectiveFrom;

    @Schema(description = "有效期止")
    private LocalDate effectiveTo;
}
