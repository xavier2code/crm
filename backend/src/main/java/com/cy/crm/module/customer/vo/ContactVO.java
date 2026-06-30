package com.cy.crm.module.customer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "联系人响应")
public class ContactVO {

    @Schema(description = "联系人ID")
    private Long id;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "职务")
    private String title;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "类型：1=重要决策人 2=业务对接人 3=操作员")
    private Integer contactType;

    @Schema(description = "类型名称")
    private String contactTypeName;

    @Schema(description = "是否主联系人")
    private Integer isPrimary;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
