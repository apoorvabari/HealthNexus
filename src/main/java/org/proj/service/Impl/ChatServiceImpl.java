package org.proj.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proj.dto.AllowedReceiversResponse;
import org.proj.dto.ChatContactDto;
import org.proj.dto.ChatMessageDto;
import org.proj.dto.SendChatMessageRequest;
import org.proj.entity.ChatMessageEntity;
import org.proj.entity.NotificationEntity;
import org.proj.entity.UserEntity;
import org.proj.repository.ChatMessageRepo;
import org.proj.security.SecurityUtils;
import org.proj.service.AdminService;
import org.proj.service.ChatService;
import org.proj.service.DoctorService;
import org.proj.service.NotificationService;
import org.proj.service.PatientService;
import org.proj.service.ReceptionistService;
import org.proj.service.UserService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepo chatMessageRepo;
    private final UserService userService;
    private final DoctorService doctorService;
    private final ReceptionistService receptionistService;
    private final PatientService patientService;
    private final AdminService adminService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    @Override
    @Transactional
    public ChatMessageDto sendMessage(SendChatMessageRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Send chat message request cannot be null");
            }
            if (request.getRecipientId() == null) {
                throw new IllegalArgumentException("Recipient ID is required");
            }
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                throw new IllegalArgumentException("Message text cannot be empty");
            }

            UserEntity sender = SecurityUtils.getCurrentUser();
            if (sender == null) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "User must be logged in to send messages");
            }

            UserEntity recipient = userService.findUserById(request.getRecipientId());

            if (sender.getId().equals(recipient.getId())) {
                throw new IllegalArgumentException("Cannot send message to yourself");
            }

            String senderRole = sender.getRole() != null ? sender.getRole().getRoleName().toUpperCase() : "PATIENT";
            String recipientRole = recipient.getRole() != null ? recipient.getRole().getRoleName().toUpperCase() : "PATIENT";

            boolean isAllowed;
            switch (senderRole) {
                case "ADMIN":
                case "SUPER_ADMIN":
                    isAllowed = Set.of("DOCTOR", "PATIENT", "RECEPTIONIST").contains(recipientRole);
                    break;
                case "PATIENT":
                    isAllowed = Set.of("DOCTOR", "RECEPTIONIST", "ADMIN", "SUPER_ADMIN").contains(recipientRole);
                    break;
                case "DOCTOR":
                    isAllowed = Set.of("ADMIN", "SUPER_ADMIN", "RECEPTIONIST", "PATIENT").contains(recipientRole);
                    break;
                case "RECEPTIONIST":
                    isAllowed = Set.of("DOCTOR", "PATIENT", "ADMIN", "SUPER_ADMIN").contains(recipientRole);
                    break;
                default:
                    isAllowed = true;
                    break;
            }

            if (!isAllowed) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Communication between role " + senderRole + " and " + recipientRole + " is not permitted");
            }

            ChatMessageEntity entity = ChatMessageEntity.builder()
                    .sender(sender)
                    .recipient(recipient)
                    .message(request.getMessage().trim())
                    .isRead(false)
                    .build();

            ChatMessageEntity saved = chatMessageRepo.save(entity);
            ChatMessageDto dto = toDto(saved);

            // 1. Deliver real-time message via STOMP user topic /topic/chat/{recipientId}
            try {
                messagingTemplate.convertAndSend("/topic/chat/" + recipient.getId(), dto);
            } catch (Exception e) {
                log.warn("Failed to send WebSocket chat message to recipient {}: {}", recipient.getId(), e.getMessage());
            }

            // Also echo to sender's /topic/chat/{senderId} for multi-device sync
            try {
                messagingTemplate.convertAndSend("/topic/chat/" + sender.getId(), dto);
            } catch (Exception e) {
                log.warn("Failed to echo WebSocket chat message to sender {}: {}", sender.getId(), e.getMessage());
            }

            // 2. Also send real-time notification alert so if recipient is not in chat
            // view, they get a toast/bell
            try {
                notificationService.createNotification(
                        recipient,
                        "New message from " + getFullName(sender),
                        truncateMessage(request.getMessage()),
                        NotificationEntity.NotificationType.GENERAL);
            } catch (Exception e) {
                log.warn("Could not push general notification for chat message: {}", e.getMessage());
            }

            return dto;
        } catch (org.springframework.security.access.AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to send chat message";
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    @Transactional
    public List<ChatMessageDto> getConversation(UUID otherUserId) {
        try {
            if (otherUserId == null) {
                throw new IllegalArgumentException("Target user ID is required");
            }

            UserEntity currentUser = SecurityUtils.getCurrentUser();
            if (currentUser == null) {
                throw new org.springframework.security.access.AccessDeniedException("Authentication required");
            }

            // Immediately mark all incoming unread messages from this sender as read in DB
            chatMessageRepo.markConversationAsRead(currentUser.getId(), otherUserId);

            List<ChatMessageEntity> messages = chatMessageRepo.findConversation(currentUser.getId(), otherUserId);

            return messages != null
                    ? messages.stream().map(this::toDto).collect(Collectors.toList())
                    : Collections.emptyList();
        } catch (org.springframework.security.access.AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve conversation";
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatContactDto> getContacts() {
        try {
            UserEntity currentUser = SecurityUtils.getCurrentUser();
            if (currentUser == null) {
                throw new org.springframework.security.access.AccessDeniedException("Authentication required");
            }

            String senderRole = currentUser.getRole() != null ? currentUser.getRole().getRoleName().toUpperCase() : "PATIENT";
            final Set<String> allowedRoles;
            switch (senderRole) {
                case "ADMIN":
                case "SUPER_ADMIN":
                    allowedRoles = Set.of("DOCTOR", "PATIENT", "RECEPTIONIST");
                    break;
                case "PATIENT":
                    allowedRoles = Set.of("DOCTOR", "RECEPTIONIST", "ADMIN", "SUPER_ADMIN");
                    break;
                case "DOCTOR":
                    allowedRoles = Set.of("ADMIN", "SUPER_ADMIN", "RECEPTIONIST", "PATIENT");
                    break;
                case "RECEPTIONIST":
                    allowedRoles = Set.of("DOCTOR", "PATIENT", "ADMIN", "SUPER_ADMIN");
                    break;
                default:
                    allowedRoles = Set.of("DOCTOR", "PATIENT", "RECEPTIONIST", "ADMIN", "SUPER_ADMIN");
                    break;
            }

            boolean isAdminSender = "ADMIN".equals(senderRole) || "SUPER_ADMIN".equals(senderRole);

            List<UserEntity> rawUsers = userService.findAllUsers();
            List<UserEntity> users = rawUsers != null
                    ? rawUsers.stream()
                            .filter(u -> !u.getId().equals(currentUser.getId()))
                            .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                            .filter(u -> {
                                boolean active = Boolean.TRUE.equals(u.getIsActive());
                                // Admin can see blocked doctors for monitoring
                                if (!active && isAdminSender) {
                                    String roleName = u.getRole() != null ? u.getRole().getRoleName().toUpperCase() : "";
                                    return "DOCTOR".equals(roleName);
                                }
                                return active;
                            })
                            .filter(u -> {
                                String r = u.getRole() != null ? u.getRole().getRoleName().toUpperCase() : "";
                                return allowedRoles.contains(r);
                            })
                            .collect(Collectors.toList())
                    : Collections.emptyList();

            List<ChatContactDto> contacts = new ArrayList<>();
            for (UserEntity u : users) {
                String role = u.getRole() != null ? u.getRole().getRoleName().toUpperCase() : "USER";
                String extraInfo = resolveExtraInfo(u, role);
                boolean isBlocked = Boolean.FALSE.equals(u.getIsActive());

                // Fetch last message if any
                List<ChatMessageEntity> convo = chatMessageRepo.findConversation(currentUser.getId(), u.getId());
                String lastMsg = null;
                String lastTime = null;
                long unread = 0;

                if (convo != null && !convo.isEmpty()) {
                    ChatMessageEntity latest = convo.get(convo.size() - 1);
                    lastMsg = latest != null ? latest.getMessage() : null;
                    if (latest != null && latest.getCreatedAt() != null) {
                        lastTime = latest.getCreatedAt().format(TIME_FORMATTER);
                    }
                    unread = convo.stream()
                            .filter(m -> m != null && m.getRecipient() != null
                                    && m.getRecipient().getId().equals(currentUser.getId())
                                    && !Boolean.TRUE.equals(m.getIsRead()))
                            .count();
                }

                contacts.add(ChatContactDto.builder()
                        .id(u.getId())
                        .name(getFullName(u))
                        .email(u.getEmail())
                        .role(role)
                        .extraInfo(extraInfo)
                        .avatar(u.getProfilePicture())
                        .unreadCount(unread)
                        .lastMessage(lastMsg)
                        .lastMessageTime(lastTime)
                        .isBlocked(isBlocked)
                        .build());
            }

            // Sort contacts: those with messages/unread first, then alphabetical
            contacts.sort((a, b) -> {
                if (a.getUnreadCount() != b.getUnreadCount()) {
                    return Long.compare(b.getUnreadCount(), a.getUnreadCount());
                }
                if (a.getLastMessage() != null && b.getLastMessage() == null)
                    return -1;
                if (a.getLastMessage() == null && b.getLastMessage() != null)
                    return 1;
                String nameA = a.getName() != null ? a.getName() : "";
                String nameB = b.getName() != null ? b.getName() : "";
                return nameA.compareToIgnoreCase(nameB);
            });

            return contacts;
        } catch (org.springframework.security.access.AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve contacts";
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AllowedReceiversResponse getAllowedReceivers() {
        try {
            UserEntity currentUser = SecurityUtils.getCurrentUser();
            if (currentUser == null) {
                throw new org.springframework.security.access.AccessDeniedException("Authentication required");
            }

            String senderRole = currentUser.getRole() != null
                    ? currentUser.getRole().getRoleName().toUpperCase()
                    : "PATIENT";

            List<String> allowedRoles;
            switch (senderRole) {
                case "ADMIN":
                case "SUPER_ADMIN":
                    allowedRoles = List.of("DOCTOR", "RECEPTIONIST", "PATIENT");
                    break;
                case "PATIENT":
                    allowedRoles = List.of("DOCTOR", "RECEPTIONIST", "ADMIN");
                    break;
                case "DOCTOR":
                    allowedRoles = List.of("PATIENT", "RECEPTIONIST", "ADMIN");
                    break;
                case "RECEPTIONIST":
                    allowedRoles = List.of("DOCTOR", "PATIENT", "ADMIN");
                    break;
                default:
                    allowedRoles = List.of("DOCTOR", "PATIENT", "RECEPTIONIST", "ADMIN");
                    break;
            }

            // Get the full flat contact list (already role-filtered) and group by role
            List<ChatContactDto> allContacts = getContacts();

            Map<String, List<ChatContactDto>> grouped = new java.util.LinkedHashMap<>();
            for (String role : allowedRoles) {
                grouped.put(role, new ArrayList<>());
            }
            for (ChatContactDto contact : allContacts) {
                String contactRole = contact.getRole() != null ? contact.getRole().toUpperCase() : "";
                // Normalise SUPER_ADMIN → ADMIN bucket for display
                String bucketKey = contactRole.equals("SUPER_ADMIN") ? "ADMIN" : contactRole;
                if (grouped.containsKey(bucketKey)) {
                    grouped.get(bucketKey).add(contact);
                }
            }

            return AllowedReceiversResponse.builder()
                    .currentRole(senderRole)
                    .allowedRoles(allowedRoles)
                    .receivers(grouped)
                    .build();
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve allowed receivers";
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    @Transactional
    public void markConversationAsRead(UUID senderId) {
        try {
            if (senderId == null) {
                throw new IllegalArgumentException("Sender ID is required");
            }

            UserEntity currentUser = SecurityUtils.getCurrentUser();
            if (currentUser == null) {
                throw new AccessDeniedException("Authentication required");
            }

            chatMessageRepo.markConversationAsRead(currentUser.getId(), senderId);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to mark conversation as read";
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        try {
            UserEntity currentUser = SecurityUtils.getCurrentUser();
            return currentUser != null ? chatMessageRepo.countUnreadMessages(currentUser.getId()) : 0L;
        } catch (Exception e) {
            log.error("Error retrieving unread count: {}", e.getMessage());
            return 0L;
        }
    }


    private String resolveExtraInfo(UserEntity user, String role) {
        try {
            switch (role) {
                case "DOCTOR":
                    return doctorService.findDoctorEntityByAccountId(user.getId())
                            .map(d -> (d.getSpecialization() != null ? d.getSpecialization() : "Doctor") +
                                    (d.getHospital() != null ? " • " + d.getHospital().getHospitalName() : ""))
                            .orElse("Doctor");
                case "RECEPTIONIST":
                    return receptionistService.findReceptionistEntityByAccountId(user.getId())
                            .map(r -> "Front Desk"
                                    + (r.getHospital() != null ? " • " + r.getHospital().getHospitalName() : ""))
                            .orElse("Receptionist");
                case "ADMIN":
                    return adminService.findAdminEntityByAccountId(user.getId())
                            .map(a -> "Administrator"
                                    + (a.getHospital() != null ? " • " + a.getHospital().getHospitalName() : ""))
                            .orElse("Hospital Admin");
                case "PATIENT":
                    return patientService.findOptionalPatientByAccountId(user.getId())
                            .map(p -> "Patient" + (p.getPatientCode() != null ? " • " + p.getPatientCode() : ""))
                            .orElse("Patient");
                default:
                    return role;
            }
        } catch (Exception e) {
            return role;
        }
    }

    private ChatMessageDto toDto(ChatMessageEntity entity) {
        return ChatMessageDto.builder()
                .id(entity.getId())
                .senderId(entity.getSender().getId())
                .senderName(getFullName(entity.getSender()))
                .senderRole(entity.getSender().getRole() != null ? entity.getSender().getRole().getRoleName() : "USER")
                .recipientId(entity.getRecipient().getId())
                .recipientName(getFullName(entity.getRecipient()))
                .recipientRole(entity.getRecipient().getRole() != null ? entity.getRecipient().getRole().getRoleName()
                        : "USER")
                .message(entity.getMessage())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String getFullName(UserEntity u) {
        StringBuilder sb = new StringBuilder();
        if (u.getFirstName() != null && !u.getFirstName().isBlank()) {
            sb.append(u.getFirstName().trim());
        }
        if (u.getLastName() != null && !u.getLastName().isBlank()) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(u.getLastName().trim());
        }
        return sb.length() > 0 ? sb.toString() : (u.getEmail() != null ? u.getEmail() : "User");
    }

    private String truncateMessage(String msg) {
        if (msg == null)
            return "";
        return msg.length() > 80 ? msg.substring(0, 77) + "..." : msg;
    }
}
