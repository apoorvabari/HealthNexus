package org.proj.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.proj.dto.AllowedReceiversResponse;
import org.proj.dto.ChatContactDto;
import org.proj.dto.ChatMessageDto;
import org.proj.dto.SendChatMessageRequest;
import org.proj.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMessageDto> sendMessage(@Valid @RequestBody SendChatMessageRequest request) {
        ChatMessageDto response = chatService.sendMessage(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/conversation/{otherUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageDto>> getConversation(@PathVariable UUID otherUserId) {
        return ResponseEntity.ok(chatService.getConversation(otherUserId));
    }

    @GetMapping("/contacts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatContactDto>> getContacts() {
        return ResponseEntity.ok(chatService.getContacts());
    }

    @GetMapping("/allowed-receivers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AllowedReceiversResponse> getAllowedReceivers() {
        return ResponseEntity.ok(chatService.getAllowedReceivers());
    }

    @PostMapping({"/read/{senderId}", "/conversations/{senderId}/read"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable UUID senderId) {
        chatService.markConversationAsRead(senderId);
        return ResponseEntity.ok(Map.of("message", "Conversation marked as read"));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", chatService.getUnreadCount()));
    }
}
