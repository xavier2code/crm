package com.cy.crm.module.notification.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.module.notification.entity.Notification;
import com.cy.crm.module.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "站内信通知相关接口")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/unread")
    @Operation(summary = "获取未读通知列表")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<List<Notification>> getUnreadNotifications() {
        Long userId = requireCurrentUserId();
        return ApiResult.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping
    @Operation(summary = "分页查询通知列表")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<Page<Notification>> pageNotifications(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size,
            @RequestParam(required = false) Integer status) {
        // 手动限制分页大小，防止 DoS 攻击
        if (size != null && size > 100) {
            size = 100L;
        }
        Long userId = requireCurrentUserId();
        return ApiResult.ok(notificationService.page(new Page<>(current, size),
                new QueryWrapper<Notification>()
                        .eq("user_id", userId)
                        .eq(status != null, "status", status)
                        .orderByDesc("created_at")));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记通知为已读")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<Void> markAsRead(@PathVariable Long id) {
        // 校验通知属于当前用户，防止越权
        Long userId = requireCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ApiResult.ok();
    }

    @PostMapping("/read-all")
    @Operation(summary = "标记所有通知为已读")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<Void> markAllAsRead() {
        Long userId = requireCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ApiResult.ok();
    }

    @GetMapping("/count/unread")
    @Operation(summary = "获取未读通知数量")
    @PreAuthorize("isAuthenticated()")
    public ApiResult<Long> getUnreadCount() {
        Long userId = requireCurrentUserId();
        return ApiResult.ok(notificationService.count(new QueryWrapper<Notification>()
                .eq("user_id", userId)
                .eq("status", 1)));
    }

    private Long requireCurrentUserId() {
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            throw com.cy.crm.common.exception.BusinessException.unauthorized();
        }
        return userId;
    }
}
