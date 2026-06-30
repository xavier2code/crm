package com.cy.crm.module.customer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "客户响应")
public class CustomerVO {

    @Schema(description = "客户ID")
    private Long id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "单位ID")
    private Long unitId;

    @Schema(description = "单位名称")
    private String unitName;

    @Schema(description = "警种")
    private String policeType;

    @Schema(description = "警种名称")
    private String policeTypeName;

    @Schema(description = "客户分层")
    private String customerLayer;

    @Schema(description = "跟进人ID")
    private Long ownerUserId;

    @Schema(description = "跟进人姓名")
    private String ownerUserName;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "联系人列表")
    private List<ContactVO> contacts;
}
