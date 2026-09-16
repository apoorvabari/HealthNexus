package org.proj.service;

import org.proj.dto.NotificationResponse;
import org.proj.dto.PageResponse;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.entity.UserEntity;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    /**
     * Internal use only — called by backend services after business events.
     * Never exposed publicly through a POST endpoint.
     */
    void createNotification(UserEntity recipient, String title, String message, NotificationType type);

    /**
     * Returns paginated notifications for the currently authenticated user,
     * newest first. Recipient is derived from JWT — never trusted from frontend.
     */
    PageResponse<NotificationResponse> getMyNotifications(int page, int size);

    /**
     * Returns all unread notifications for the currently authenticated user.
     */
    List<NotificationResponse> getMyUnreadNotifications();

    /**
     * Marks a single notification as read.
     * Verifies that the notification belongs to the current user.
     */
    NotificationResponse markAsRead(UUID notificationId);

    /**
     * Marks ALL notifications for the current user as read.
     */
    void markAllAsRead();
}
