package org.proj.service;

import org.proj.dto.NotificationResponse;
import org.proj.dto.PageResponse;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.entity.UserEntity;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void createNotification(UserEntity recipient, String title, String message, NotificationType type);

    PageResponse<NotificationResponse> getMyNotifications(int page, int size);

    List<NotificationResponse> getMyUnreadNotifications();

    NotificationResponse markAsRead(UUID notificationId);

    void markAllAsRead();
}
