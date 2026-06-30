package com.cy.crm.common.aspect;

import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.AuditLogMapper;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

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
            saveAuditLog(joinPoint, executeTime, exception);
        }
    }

    private void saveAuditLog(ProceedingJoinPoint joinPoint, long executeTime, Exception exception) {
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
                    auditLog.setParams(objectMapper.writeValueAsString(args));
                } catch (Exception e) {
                    auditLog.setParams(args.length + " params");
                }
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
}
