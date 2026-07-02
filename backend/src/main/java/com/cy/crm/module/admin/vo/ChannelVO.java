package com.cy.crm.module.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "渠道视图")
public class ChannelVO {
    @Schema(description = "渠道ID")
    private Long id;

    @Schema(description = "渠道名称")
    private String name;

    @Schema(description = "所属区域")
    private String region;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Schema(description = "渠道负责人列表")
    private List<UserVO> heads;

    @Schema(description = "渠道 BD 列表")
    private List<UserVO> bds;
}
