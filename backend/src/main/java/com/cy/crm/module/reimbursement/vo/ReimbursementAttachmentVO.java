package com.cy.crm.module.reimbursement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "报销附件响应")
public class ReimbursementAttachmentVO {

    @Schema(description = "附件ID")
    private Long id;

    @Schema(description = "原文件名")
    private String fileName;

    @Schema(description = "存储路径（相对路径）")
    private String filePath;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "MIME 类型")
    private String contentType;

    @Schema(description = "上传人ID")
    private Long uploadedBy;

    @Schema(description = "上传时间")
    private LocalDateTime uploadedAt;
}
