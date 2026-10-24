package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatContactDto {
    private UUID id;
    private String name;
    private String email;
    private String role;
    private String extraInfo; // e.g., specialization, department, hospital
    private String avatar;
    private long unreadCount;
    private String lastMessage;
    private String lastMessageTime;
    private Boolean isBlocked;
}
