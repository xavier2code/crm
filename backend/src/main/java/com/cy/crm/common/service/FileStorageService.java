package com.cy.crm.common.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 本地文件存储服务
 *
 * 设计：
 * - 文件落地路径：{basePath}/{yyyy-MM}/{uuid}.{ext}
 * - 数据库只保存相对路径（不含 basePath），便于备份/迁移
 * - 所有写操作均做白名单 + 大小校验，失败抛 6018
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageProperties props;

    /**
     * 存储单个文件，返回相对路径（写入数据库用）
     */
    public StoredFile store(MultipartFile file) {
        validate(file);
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String ext = extractExtension(original);
        String relativeDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String storedFileName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String relativePath = relativeDir + "/" + storedFileName;

        try {
            Path target = Paths.get(props.getBasePath()).resolve(relativePath).toAbsolutePath().normalize();
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Stored file: original={} path={} size={}", original, relativePath, file.getSize());
            return new StoredFile(original, relativePath, file.getSize(), file.getContentType());
        } catch (IOException e) {
            log.error("Failed to store file: {}", original, e);
            throw BusinessException.reimbursementAttachmentUploadFailed(e.getMessage());
        }
    }

    /**
     * 解析数据库中的相对路径为绝对路径
     */
    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            throw BusinessException.reimbursementAttachmentUploadFailed("文件路径为空");
        }
        Path base = Paths.get(props.getBasePath()).toAbsolutePath().normalize();
        Path target = base.resolve(relativePath).normalize();
        if (!target.startsWith(base)) {
            throw BusinessException.reimbursementAttachmentUploadFailed("非法的文件路径");
        }
        return target;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.reimbursementAttachmentUploadFailed("文件为空");
        }
        if (file.getSize() > props.getMaxFileSize()) {
            throw BusinessException.reimbursementAttachmentUploadFailed(
                    "文件大小超过限制 " + (props.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType();
        List<String> allowed = props.getAllowedTypes();
        if (contentType != null && !allowed.contains(contentType)) {
            throw BusinessException.reimbursementAttachmentUploadFailed("不支持的文件类型: " + contentType);
        }
    }

    public long maxFileSize() {
        return props.getMaxFileSize();
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        String ext = filename.substring(dot + 1);
        if (ext.length() > 10) return "";
        return ext.toLowerCase();
    }

    public record StoredFile(String originalName, String relativePath, long size, String contentType) {}
}
