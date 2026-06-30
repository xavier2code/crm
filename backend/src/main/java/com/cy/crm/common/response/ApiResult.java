package com.cy.crm.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 统一响应包装器
 * 符合开发文档 §33 规范
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应结构")
public class ApiResult<T> {
    @Schema(description = "业务状态码，0 表示成功", example = "0")
    private int code;

    @Schema(description = "是否成功", example = "true")
    private boolean success;

    @Schema(description = "提示信息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "追踪ID，用于问题排查", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String traceId;

    @Schema(description = "响应时间戳", example = "2026-06-30T12:00:00")
    private LocalDateTime timestamp;

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(0);
        result.setSuccess(true);
        result.setMessage("success");
        result.setData(data);
        result.setTraceId(getTraceId());
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    /**
     * 业务错误响应（指定错误码）
     * 符合开发文档 §20 错误码体系
     */
    public static <T> ApiResult<T> error(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setSuccess(false);
        result.setMessage(message);
        result.setData(null);
        result.setTraceId(getTraceId());
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

    /**
     * 业务错误响应（带额外数据）
     * 用于需要返回上下文信息的场景（如错误详情、提示等）
     */
    public static <T> ApiResult<T> error(int code, String message, T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setSuccess(false);
        result.setMessage(message);
        result.setData(data);
        result.setTraceId(getTraceId());
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

    /**
     * 通用错误响应（默认500）
     */
    public static <T> ApiResult<T> error(String message) {
        return error(1099, message);
    }

    /**
     * 成功响应（别名方法）
     */
    public static <T> ApiResult<T> success(T data) {
        return ok(data);
    }

    /**
     * 成功响应（无数据，别名方法）
     */
    public static <T> ApiResult<T> success() {
        return ok(null);
    }

    /**
     * 获取当前请求的追踪ID
     * 优先从 MDC 获取，否则生成新的 UUID
     */
    private static String getTraceId() {
        String traceId = MDC.get("traceId");
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
