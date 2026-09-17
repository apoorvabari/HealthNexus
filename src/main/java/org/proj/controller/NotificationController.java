package org.proj.controller;

import org.proj.dto.NotificationResponse;
import org.proj.dto.PageResponse;
import org.proj.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PageResponse<NotificationResponse> response = notificationService.getMyNotifications(page, size);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to fetch notifications");
        }
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyUnreadNotifications() {
        try {
            List<NotificationResponse> response = notificationService.getMyUnreadNotifications();
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to fetch unread notifications");
        }
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAsRead(@PathVariable UUID id) {
        try {
            NotificationResponse response = notificationService.markAsRead(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to mark notification as read");
        }
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAllAsRead() {
        try {
            notificationService.markAllAsRead();
            Map<String, String> response = new HashMap<>();
            response.put("message", "All notifications marked as read");
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to mark all notifications as read");
        }
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return new ResponseEntity<>(response, status);
    }
}
