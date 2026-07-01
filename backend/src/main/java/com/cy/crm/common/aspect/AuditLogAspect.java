package com.cy.crm.common.aspect;

import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.AuditLogMapper;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.common.util.IpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    // Sensitive field name patterns to mask
    private static final Set<String> SENSITIVE_FIELD_PATTERNS = new HashSet<>(Arrays.asList(
            "password", "passwd", "pwd",
            "token", "accessToken", "refreshToken", "apiKey", "secret",
            "captcha", "captchaCode", "captchaUuid",
            "creditCard", "ssn", "idCard"
    ));

    @Pointcut("@annotation(com.cy.crm.common.annotation.AuditLog)")
    public void auditLog() {}

    @Around("auditLog()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        // Extract context-dependent data in MAIN thread before async execution
        HttpServletRequest request = getRequest();
        Long userId = currentUserService.getCurrentUserId();
        String username = null;
        User user = currentUserService.getCurrentUser();
        if (user != null) {
            username = user.getUsername();
        }
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String params = null;
        if (args != null && args.length > 0) {
            try {
                params = sanitizeParams(args);
            } catch (Exception e) {
                params = args.length + " params";
            }
        } else {
            params = "";
        }
        String ip = getIpAddress(request);

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executeTime = System.currentTimeMillis() - startTime;
            // Pass all extracted context as parameters
            saveAuditLogAsync(userId, username, className, methodName, params, ip, executeTime, exception);
        }
    }

    @Async("auditLogExecutor")
    private void saveAuditLogAsync(Long userId, String username, String className, String methodName,
                                    String params, String ip, long executeTime, Exception exception) {
        try {
            com.cy.crm.module.admin.entity.AuditLog auditLog = new com.cy.crm.module.admin.entity.AuditLog();

            // Use pre-extracted context from main thread
            auditLog.setUserId(userId != null ? userId : 0L);
            auditLog.setUsername(username != null ? username : "system");
            auditLog.setModule(className);
            auditLog.setMethod(methodName);
            auditLog.setOperation(getOperationName(methodName));
            auditLog.setParams(params);
            auditLog.setIp(ip);
            auditLog.setStatus(exception == null ? 1 : 0);
            auditLog.setErrorMsg(exception != null ? exception.getMessage() : null);
            auditLog.setExecuteTime((int) executeTime);
            auditLog.setCreatedAt(LocalDateTime.now());

            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("保存审计日志失败", e);
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getIpAddress(HttpServletRequest request) {
        return IpUtils.getClientIp(request);
    }

    private String getOperationName(String methodName) {
        if (methodName.startsWith("create") || methodName.startsWith("add") || methodName.startsWith("save")) {
            return "新增";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify") || methodName.startsWith("edit")) {
            return "修改";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "删除";
        } else if (methodName.startsWith("get") || methodName.startsWith("find") || methodName.startsWith("query") || methodName.startsWith("list")) {
            return "查询";
        } else if (methodName.startsWith("approve") || methodName.startsWith("reject")) {
            return "审批";
        }
        return "其他";
    }

    /**
     * Sanitize method parameters by masking sensitive fields
     * @param args Method arguments array
     * @return JSON string with sensitive fields masked
     */
    private String sanitizeParams(Object[] args) {
        if (args == null) {
            return "";
        }

        if (args.length == 0) {
            return "[]";
        }

        try {
            // Convert args to JSON array
            com.fasterxml.jackson.databind.node.ArrayNode arrayNode = objectMapper.createArrayNode();

            for (Object arg : args) {
                if (arg == null) {
                    arrayNode.addNull();
                } else if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                    // Handle primitive types directly
                    arrayNode.addPOJO(arg);
                } else {
                    // Convert to JSON node and sanitize
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.valueToTree(arg);
                    sanitizeJsonNode(node);
                    arrayNode.add(node);
                }
            }

            return objectMapper.writeValueAsString(arrayNode);
        } catch (Exception e) {
            log.warn("Failed to sanitize params, using fallback", e);
            return args.length + " params (sanitized)";
        }
    }

    /**
     * Recursively sanitize a JSON node by masking sensitive fields
     * @param node The JSON node to sanitize
     */
    private void sanitizeJsonNode(com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            // Sanitize fields
            objectNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey().toLowerCase();
                boolean isSensitive = SENSITIVE_FIELD_PATTERNS.stream()
                        .anyMatch(pattern -> fieldName.contains(pattern.toLowerCase()));

                if (isSensitive) {
                    // Mask the value
                    objectNode.put(entry.getKey(), "******");
                } else {
                    // Recursively sanitize nested objects
                    sanitizeJsonNode(entry.getValue());
                }
            });
        } else if (node.isArray()) {
            // Recursively sanitize array elements
            for (com.fasterxml.jackson.databind.JsonNode item : node) {
                sanitizeJsonNode(item);
            }
        }
    }
}
