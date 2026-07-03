package com.cy.crm.module.reimbursement.controller;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.common.service.FileStorageService;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.module.reimbursement.entity.ReimbursementAttachment;
import com.cy.crm.module.reimbursement.service.ReimbursementService;
import com.cy.crm.module.reimbursement.vo.ReimbursementAttachmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Tag(name = "报销附件管理")
@RestController
@RequestMapping("/api/reimbursements")
@RequiredArgsConstructor
public class ReimbursementAttachmentController {

    private final ReimbursementService reimbursementService;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "上传附件")
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('reimbursement:create', 'reimbursement:approve', 'reimbursement:pay', 'reimbursement:view')")
    public ApiResult<ReimbursementAttachmentVO> upload(@PathVariable Long id,
                                                       @RequestParam("file") MultipartFile file) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResult.success(reimbursementService.uploadAttachment(id, file, userId));
    }

    @Operation(summary = "查询附件列表")
    @GetMapping("/{id}/attachments")
    @PreAuthorize("hasAnyAuthority('reimbursement:view', 'reimbursement:create', 'reimbursement:approve', 'reimbursement:pay')")
    public ApiResult<List<ReimbursementAttachmentVO>> list(@PathVariable Long id) {
        return ApiResult.success(reimbursementService.listAttachments(id));
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAnyAuthority('reimbursement:create')")
    public ApiResult<Void> delete(@PathVariable Long attachmentId) {
        Long userId = currentUserService.getCurrentUserId();
        reimbursementService.deleteAttachment(attachmentId, userId);
        return ApiResult.success();
    }

    @Operation(summary = "下载附件")
    @GetMapping("/attachments/{attachmentId}/download")
    @PreAuthorize("hasAnyAuthority('reimbursement:view', 'reimbursement:create', 'reimbursement:approve', 'reimbursement:pay')")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) throws IOException {
        ReimbursementAttachment att = reimbursementService.getAttachmentEntity(attachmentId);
        Path path = fileStorageService.resolve(att.getFilePath());
        File file = path.toFile();
        if (!file.exists()) {
            throw BusinessException.reimbursementAttachmentUploadFailed("文件不存在: " + att.getFileName());
        }
        String encoded = URLEncoder.encode(att.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        String contentType = att.getContentType() != null ? att.getContentType()
                : Files.probeContentType(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .contentLength(file.length())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .body(new FileSystemResource(file));
    }
}
