package com.cy.crm.module.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.module.notification.entity.Notification;
import com.cy.crm.module.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    public void createNotification(Long userId, String title, String content, String type, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setStatus(1);
        notification.setRelatedId(relatedId);
        notification.setCreatedAt(LocalDateTime.now());
        save(notification);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return list(new QueryWrapper<Notification>()
                .eq("user_id", userId)
                .eq("status", 1)
                .orderByDesc("created_at"));
    }

    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = getById(notificationId);
        if (notification == null) {
            return;
        }
        if (!notification.getUserId().equals(userId)) {
            throw com.cy.crm.common.exception.BusinessException.forbidden();
        }
        notification.setStatus(2);
        notification.setReadAt(LocalDateTime.now());
        updateById(notification);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = getUnreadNotifications(userId);
        unread.forEach(n -> {
            n.setStatus(2);
            n.setReadAt(LocalDateTime.now());
        });
        updateBatchById(unread);
    }
}
