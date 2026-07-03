package com.cy.crm.common.aspect;

import com.cy.crm.module.admin.entity.AuditLog;
import com.cy.crm.module.admin.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计日志异步写入器
 *
 * 必须独立为单独的 Spring Bean，因为 {@code @Async} 基于 AOP 代理实现：
 * <ul>
 *   <li>{@code private} 方法无法被代理</li>
 *   <li>同类内部自调用（self-invocation）也绕过代理</li>
 * </ul>
 * 将异步方法提取到独立 Bean 后，Aspect 通过注入的代理对象调用即可正确异步执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogMapper auditLogMapper;

    @Async("auditLogExecutor")
    public void saveAsync(Long userId, String username, String className, String methodName,
                          String params, String ip, long executeTime, Exception exception) {
        try {
            AuditLog auditLog = new AuditLog();
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
