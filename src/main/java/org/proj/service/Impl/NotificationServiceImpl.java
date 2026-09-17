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
        if (recipient == null) {
            return;
        }
        NotificationEntity notification = NotificationEntity.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .build();
        notificationRepo.save(notification);

        messagingTemplate.convertAndSend("/topic/notifications/" + recipient.getId(), toResponse(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(int page, int size) {
        UserEntity currentUser = requireCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationEntity> notificationPage =
                notificationRepo.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable);

        List<NotificationResponse> content = notificationPage.getContent()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyUnreadNotifications() {
        UserEntity currentUser = requireCurrentUser();
        return notificationRepo
                .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        UserEntity currentUser = requireCurrentUser();
        NotificationEntity notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to mark this notification as read");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepo.save(notification);
        }

        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        UserEntity currentUser = requireCurrentUser();
        notificationRepo.markAllAsReadByRecipientId(currentUser.getId());
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
