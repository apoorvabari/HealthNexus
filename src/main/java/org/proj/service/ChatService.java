package org.proj.service;

import org.proj.dto.AllowedReceiversResponse;
import org.proj.dto.ChatContactDto;
import org.proj.dto.ChatMessageDto;
import org.proj.dto.SendChatMessageRequest;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatMessageDto sendMessage(SendChatMessageRequest request);

    List<ChatMessageDto> getConversation(UUID otherUserId);

    List<ChatContactDto> getContacts();

    AllowedReceiversResponse getAllowedReceivers();

    void markConversationAsRead(UUID senderId);

    long getUnreadCount();
}
