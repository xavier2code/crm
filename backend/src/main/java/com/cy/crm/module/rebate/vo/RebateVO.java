package com.cy.crm.module.rebate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "返利响应")
public class RebateVO {

    @Schema(description = "返利ID")
    private Long id;

    @Schema(description = "渠道ID")
    private Long channelId;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "返利率")
    private BigDecimal rebateRate;

    @Schema(description = "应发金额")
    private BigDecimal totalAmount;

    @Schema(description = "实发金额")
    private BigDecimal actualAmount;

    @Schema(description = "确认状态：1=未确认 2=已确认")
    private Integer confirmStatus;

    @Schema(description = "确认状态名称")
    private String confirmStatusName;

    @Schema(description = "付款状态：1=未付款 2=已付款")
    private Integer paymentStatus;

    @Schema(description = "付款状态名称")
    private String paymentStatusName;

    @Schema(description = "返利类型：1=业绩完成返利 2=回款返利 3=服务返利")
    private Integer rebateType;

    @Schema(description = "返利类型名称")
    private String rebateTypeName;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
