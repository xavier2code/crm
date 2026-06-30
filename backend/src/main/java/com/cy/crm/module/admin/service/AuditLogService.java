package com.cy.crm.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.module.admin.entity.AuditLog;
import com.cy.crm.module.admin.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志服务
 */
@Service
@RequiredArgsConstructor
public class AuditLogService extends ServiceImpl<AuditLogMapper, AuditLog> {

    private final AuditLogMapper auditLogMapper;

    /**
     * 分页查询审计日志
     */
    public Page<AuditLog> pageAuditLogs(Long current, Long size, Long userId, String module,
                                         String operation, LocalDate startDate, LocalDate endDate) {
        QueryWrapper<AuditLog> wrapper = new QueryWrapper<>();

        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (module != null && !module.isEmpty()) {
            wrapper.eq("module", module);
        }
        if (operation != null && !operation.isEmpty()) {
            wrapper.like("operation", operation);
        }
        if (startDate != null) {
            wrapper.ge("created_at", startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le("created_at", endDate.atTime(23, 59, 59));
        }

        wrapper.orderByDesc("created_at");

        return auditLogMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 获取最近的审计日志
     */
    public List<AuditLog> getRecentLogs(Integer limit) {
        return auditLogMapper.selectList(
                new QueryWrapper<AuditLog>()
                        .orderByDesc("created_at")
                        .last("LIMIT " + (limit != null ? limit : 20))
        );
    }

    /**
     * 获取用户操作统计
     */
    public long getUserOperationCount(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogMapper.selectCount(
                new QueryWrapper<AuditLog>()
                        .eq("user_id", userId)
                        .ge(startDate != null, "created_at", startDate)
                        .le(endDate != null, "created_at", endDate)
        );
    }
}
