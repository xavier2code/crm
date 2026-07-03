package com.cy.crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /** 本地存储根目录 */
    private String basePath = "./data/uploads";

    /** 单文件大小上限（字节） */
    private long maxFileSize = 20L * 1024 * 1024;

    /** 单次上传文件数上限 */
    private int maxFilesPerRequest = 10;

    /** 允许的 MIME 类型白名单 */
    private List<String> allowedTypes = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/zip"
    );
}
