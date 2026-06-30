package com.cy.crm.common.exception;

import com.cy.crm.common.response.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 符合开发文档 §34 规范
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========== 业务异常 ==========

    /**
     * 业务异常（使用预定义错误码）
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Object> handleBusiness(BusinessException e) {
        log.warn("业务异常 code={} msg={}", e.getCode(), e.getMessage());
        // 如果有额外数据，使用 error(code, message, data)
        if (e.getData() != null) {
            return ApiResult.error(e.getCode(), e.getMessage(), e.getData());
        }
        return ApiResult.error(e.getCode(), e.getMessage());
    }

    // ========== 参数校验异常 ==========

    /**
     * JWT参数校验异常 (@Valid @RequestBody)
     * 错误码：1001
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> details = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null ? "校验失败" : fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing + "; " + replacement
                ));
        log.warn("参数校验失败: {}", details);
        return ApiResult.error(1001, "参数校验失败", details);
    }

    /**
     * 参数绑定异常
     * 错误码：1001
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Map<String, String>> handleBind(BindException e) {
        Map<String, String> details = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null ? "校验失败" : fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing + "; " + replacement
                ));
        log.warn("参数绑定失败: {}", details);
        return ApiResult.error(1001, "参数绑定失败", details);
    }

    /**
     * 路径变量参数类型不匹配
     * 错误码：1001
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResult<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数类型错误: %s", e.getName());
        log.warn("参数类型错误: name={}, requiredType={}", e.getName(), e.getRequiredType());
        return ApiResult.error(1001, message);
    }

    /**
     * JSON解析异常
     * 错误码：1007
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleJson(HttpMessageNotReadableException e) {
        log.warn("JSON解析异常", e);
        return ApiResult.error(1007, "请求体格式错误");
    }

    // ========== 认证授权异常 ==========

    /**
     * 认证失败（401）
     * 错误码：2001
     */
    @ExceptionHandler({org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
            org.springframework.security.authentication.InsufficientAuthenticationException.class})
    public ApiResult<Void> handleAuthenticationFailed(Exception e) {
        log.warn("认证失败", e);
        return ApiResult.error(2001, "未登录或登录已过期");
    }

    /**
     * 账号密码错误
     * 错误码：2002
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ApiResult<Void> handleBadCredentials(BadCredentialsException e) {
        log.warn("账号密码错误");
        return ApiResult.error(2002, "账号或密码错误");
    }

    /**
     * 权限不足（403）
     * 错误码：2004
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResult<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足", e);
        return ApiResult.error(2004, "无权限访问该资源");
    }

    // ========== 资源/方法异常 ==========

    /**
     * 404资源不存在
     * 错误码：1002
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResult<Void> handle404(NoHandlerFoundException e) {
        log.warn("资源不存在: {}", e.getRequestURL());
        return ApiResult.error(1002, "资源不存在");
    }

    // ========== 数据库异常 ==========

    /**
     * 数据完整性约束违反
     * 错误码：1002（通用）/具体业务错误码
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResult<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        log.error("数据完整性违反", e);
        String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();

        // 根据约束名判断具体错误
        if (msg != null) {
            if (msg.contains("uk_opp_active") || msg.contains("uk_opportunity_protection")) {
                return ApiResult.error(4001, "客户已存在生效中报备");
            }
            if (msg.contains("uk_customer")) {
                return ApiResult.error(3002, "客户+警种已存在");
            }
            if (msg.contains("uk_project_opp")) {
                return ApiResult.error(5001, "项目已存在，请勿重复创建");
            }
            if (msg.contains("uq_rebate_rate")) {
                return ApiResult.error(3005, "返利率配置已存在");
            }
        }
        return ApiResult.error(1002, "数据已存在或违反约束");
    }

    /**
     * 乐观锁冲突（MyBatis-Plus）
     * 错误码：5002
     *
     * 说明：项目使用 MyBatis-Plus 而非 JPA，因此不处理 JPA 的乐观锁异常。
     * Service 层应在捕获 MyBatisPlusException 后自行转换为 BusinessException(5002)。
     */
    @ExceptionHandler(com.baomidou.mybatisplus.core.exceptions.MybatisPlusException.class)
    public ApiResult<Void> handleOptimisticLock(com.baomidou.mybatisplus.core.exceptions.MybatisPlusException e) {
        log.warn("MyBatis-Plus 异常", e);
        // 如果是乐观锁相关异常，返回 5002；否则按系统错误兜底
        String msg = e.getMessage();
        if (msg != null && (msg.contains("乐观锁") || msg.contains("optimistic lock") || msg.contains("version"))) {
            return ApiResult.error(5002, "数据已被他人修改，请刷新");
        }
        return ApiResult.error(1099, "系统内部错误");
    }

    // ========== 兜底异常 ==========

    /**
     * 未处理异常
     * 错误码：1099
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleAny(Exception e) {
        log.error("未处理异常", e);
        return ApiResult.error(1099, "系统内部错误");
    }
}
