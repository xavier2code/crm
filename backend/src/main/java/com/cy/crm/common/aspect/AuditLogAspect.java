package com.cy.crm.common.aspect;

import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.AuditLogMapper;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executeTime = System.currentTimeMillis() - startTime;
            saveAuditLogAsync(joinPoint, executeTime, exception);
        }
    }

    @Async("auditLogExecutor")
    private void saveAuditLogAsync(ProceedingJoinPoint joinPoint, long executeTime, Exception exception) {
        try {
            HttpServletRequest request = getRequest();
            if (request == null) {
                return;
            }

            com.cy.crm.module.admin.entity.AuditLog auditLog = new com.cy.crm.module.admin.entity.AuditLog();

            Long userId = currentUserService.getCurrentUserId();
            auditLog.setUserId(userId != null ? userId : 0L);

            User user = currentUserService.getCurrentUser();
            auditLog.setUsername(user != null ? user.getUsername() : "system");

            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            auditLog.setModule(className);
            auditLog.setMethod(methodName);
            auditLog.setOperation(getOperationName(methodName));

            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                try {
                    auditLog.setParams(sanitizeParams(args));
                } catch (Exception e) {
                    auditLog.setParams(args.length + " params");
                }
            } else {
                auditLog.setParams("");
            }

            auditLog.setIp(getIpAddress(request));
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
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
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
