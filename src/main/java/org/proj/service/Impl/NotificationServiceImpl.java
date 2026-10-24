package org.proj.service.impl;

import org.proj.dto.NotificationResponse;
import org.proj.dto.PageResponse;
import org.proj.entity.NotificationEntity;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.entity.UserEntity;
import org.proj.repository.NotificationRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepo notificationRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void createNotification(UserEntity recipient, String title, String message, NotificationType type) {
        try {
            if (recipient == null) {
                return;
            }
            NotificationEntity notification = NotificationEntity.builder()
                    .recipient(recipient)
                    .title(title != null ? title.trim() : "Notification")
                    .message(message != null ? message.trim() : "")
                    .type(type != null ? type : NotificationType.GENERAL)
                    .read(false)
                    .build();
            notificationRepo.save(notification);

            try {
                messagingTemplate.convertAndSend("/topic/notifications/" + recipient.getId(), toResponse(notification));
            } catch (Exception wsEx) {
                // Non-critical WebSocket push failure
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to create notification";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(int page, int size) {
        try {
            UserEntity currentUser = requireCurrentUser();
            int safePage = page < 0 ? 0 : page;
            int safeSize = size <= 0 ? 10 : size;
            Pageable pageable = PageRequest.of(safePage, safeSize);
            Page<NotificationEntity> notificationPage =
                    notificationRepo.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable);

            List<NotificationResponse> content = notificationPage != null
                    ? notificationPage.getContent().stream().map(this::toResponse).collect(Collectors.toList())
                    : List.of();

            return new PageResponse<>(
                    content,
                    notificationPage != null ? notificationPage.getNumber() : safePage,
                    notificationPage != null ? notificationPage.getSize() : safeSize,
                    notificationPage != null ? notificationPage.getTotalElements() : 0L,
                    notificationPage != null ? notificationPage.getTotalPages() : 0,
                    notificationPage != null ? notificationPage.isLast() : true
            );
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to fetch notifications";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyUnreadNotifications() {
        try {
            UserEntity currentUser = requireCurrentUser();
            List<NotificationEntity> unread = notificationRepo
                    .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(currentUser.getId());
            return unread != null
                    ? unread.stream().map(this::toResponse).collect(Collectors.toList())
                    : List.of();
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to fetch unread notifications";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        try {
            if (notificationId == null) {
                throw new IllegalArgumentException("Notification ID is required");
            }
            UserEntity currentUser = requireCurrentUser();
            NotificationEntity notification = notificationRepo.findById(notificationId)
                    .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

            if (notification.getRecipient() == null || !notification.getRecipient().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to mark this notification as read");
            }

            if (!notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(LocalDateTime.now());
                notification = notificationRepo.save(notification);
            }

            return toResponse(notification);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to mark notification as read";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        try {
            UserEntity currentUser = requireCurrentUser();
            return notificationRepo.countByRecipientIdAndReadFalse(currentUser.getId());
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        try {
            UserEntity currentUser = requireCurrentUser();
            notificationRepo.markAllAsReadByRecipientId(currentUser.getId());
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to mark all notifications as read";
            throw new RuntimeException(msg, e);
        }
    }


    private UserEntity requireCurrentUser() {
        UserEntity currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return currentUser;
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .read(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }
}
