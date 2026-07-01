package com.cy.crm.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.module.admin.entity.AuditLog;
import com.cy.crm.module.admin.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 审计日志控制器
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "系统审计日志查询接口")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "分页查询审计日志")
    public Page<AuditLog> pageAuditLogs(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") @Max(100) Long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // 手动限制分页大小，防止 DoS 攻击
        if (size != null && size > 100) {
            size = 100L;
        }
        return auditLogService.pageAuditLogs(current, size, userId, module, operation, startDate, endDate);
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近的审计日志")
    public List<AuditLog> getRecentLogs(@RequestParam(defaultValue = "20") Integer limit) {
        return auditLogService.getRecentLogs(limit);
    }

    @GetMapping("/stats/user/{userId}")
    @Operation(summary = "获取用户操作统计")
    public long getUserOperationCount(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;
        return auditLogService.getUserOperationCount(userId, start, end);
    }
}
