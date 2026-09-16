package org.proj.security;

import org.proj.entity.AdminEntity;
import org.proj.entity.UserEntity;
import org.proj.repository.AdminRepo;
import org.proj.service.TenantContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private static final String NOTIFICATION_TOPIC_PREFIX =
            "/topic/notifications/";

    private static final String WS_HOSPITAL_ATTRIBUTE =
            "HEALTHNEXUS_WS_HOSPITAL_ID";

    private static final String HOSPITAL_HEADER =
            "X-Hospital-Id";

    private static final Pattern DOCTOR_PRESENCE_TOPIC_PATTERN =
            Pattern.compile(
                    "^/topic/hospital/([0-9a-fA-F-]{36})/doctor-presence$"
            );

    private static final Pattern QUEUE_TOPIC_PATTERN =
            Pattern.compile(
                    "^/topic/hospital/([0-9a-fA-F-]{36})/queue$"
            );

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private TenantContextService tenantContextService;

    @Autowired
    private AdminRepo adminRepo;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            authenticateConnection(accessor);
            resolveAndStoreTenant(accessor);
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            validateNotificationSubscription(accessor);
            validateDoctorPresenceSubscription(accessor);
            validateQueueSubscription(accessor);
            return message;
        }

        if (StompCommand.SEND.equals(command)) {
            blockServerOnlyPublishing(accessor);
            return message;
        }

        return message;
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {

        String authorizationHeader =
                accessor.getFirstNativeHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException(
                    "WebSocket authentication required"
            );
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isBlank()) {
            throw new AccessDeniedException(
                    "WebSocket authentication required"
            );
        }

        try {
            String username = jwtUtil.extractUsername(token);

            if (username == null || username.isBlank()) {
                throw new AccessDeniedException(
                        "Invalid WebSocket authentication token"
                );
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(username);

            if (userDetails == null || !userDetails.isEnabled()) {
                throw new AccessDeniedException(
                        "Invalid WebSocket authentication token"
                );
            }

            if (!jwtUtil.validateToken(token, userDetails)) {
                throw new AccessDeniedException(
                        "Invalid WebSocket authentication token"
                );
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            accessor.setUser(authentication);

        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new AccessDeniedException(
                    "Invalid WebSocket authentication token"
            );
        }
    }

    private void resolveAndStoreTenant(
            StompHeaderAccessor accessor) {

        Authentication authentication = getAuthentication(accessor);
        UserEntity currentUser = getCurrentUser(authentication);

        UUID hospitalId = resolveWebSocketHospitalId(
                accessor,
                currentUser
        );

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "Hospital context is required"
            );
        }

        java.util.Map<String, Object> sessionAttributes =
                accessor.getSessionAttributes();

        if (sessionAttributes == null) {
            sessionAttributes = new java.util.HashMap<>();
            accessor.setSessionAttributes(sessionAttributes);
        }

        sessionAttributes.put(
                WS_HOSPITAL_ATTRIBUTE,
                hospitalId.toString()
        );
    }

    private UUID resolveWebSocketHospitalId(
            StompHeaderAccessor accessor,
            UserEntity currentUser) {

        String role = currentUser.getRole() == null
                ? ""
                : currentUser.getRole()
                        .getRoleName()
                        .trim()
                        .toUpperCase();

        if ("ADMIN".equals(role)) {

            String headerValue = accessor.getFirstNativeHeader(
                    HOSPITAL_HEADER
            );

            var assignments = adminRepo.findAllByAccountId(
                    currentUser.getId()
            ).stream()
                    .filter(a -> a.getStatus() == AdminEntity.AdminStatus.ACTIVE)
                    .filter(a -> a.getHospital() != null
                            && a.getHospital().getId() != null)
                    .toList();

            if (assignments.isEmpty()) {
                throw new AccessDeniedException(
                        "No active hospital assignment exists"
                );
            }

            if (headerValue == null || headerValue.isBlank()) {
                if (assignments.size() == 1) {
                    return assignments.get(0).getHospital().getId();
                }

                throw new AccessDeniedException(
                        "Hospital context is required for a multi-hospital administrator"
                );
            }

            final UUID requestedHospitalId;

            try {
                requestedHospitalId = UUID.fromString(
                        headerValue.trim()
                );
            } catch (IllegalArgumentException e) {
                throw new AccessDeniedException(
                        "Invalid hospital context"
                );
            }

            boolean authorized = assignments.stream()
                    .anyMatch(a -> requestedHospitalId.equals(
                            a.getHospital().getId()
                    ));

            if (!authorized) {
                throw new AccessDeniedException(
                        "You are not authorized for this hospital"
                );
            }

            return requestedHospitalId;
        }

        UUID hospitalId = tenantContextService.getHospitalIdForUser(
                currentUser
        );

        if (hospitalId == null) {
            throw new AccessDeniedException(
                    "Hospital context is required"
            );
        }

        return hospitalId;
    }

    private void validateNotificationSubscription(
            StompHeaderAccessor accessor) {

        String destination = accessor.getDestination();

        if (destination == null
                || !destination.startsWith(NOTIFICATION_TOPIC_PREFIX)) {
            return;
        }

        Authentication authentication = getAuthentication(accessor);
        UserEntity currentUser = getCurrentUser(authentication);

        String requestedUserId = destination.substring(
                NOTIFICATION_TOPIC_PREFIX.length()
        );

        final UUID requestedNotificationUserId;

        try {
            requestedNotificationUserId = UUID.fromString(
                    requestedUserId
            );
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException(
                    "Invalid notification topic"
            );
        }

        if (currentUser.getId() == null
                || !currentUser.getId().equals(
                requestedNotificationUserId)) {

            throw new AccessDeniedException(
                    "You are not authorized to subscribe to this notification topic"
            );
        }
    }

    private void validateDoctorPresenceSubscription(
            StompHeaderAccessor accessor) {

        String destination = accessor.getDestination();

        if (destination == null
                || !destination.startsWith("/topic/hospital/")) {
            return;
        }

        Matcher matcher = DOCTOR_PRESENCE_TOPIC_PATTERN.matcher(
                destination
        );

        if (!matcher.matches()) {
            if (destination.contains("/doctor-presence")) {
                throw new AccessDeniedException(
                        "Invalid doctor presence topic"
                );
            }
            return;
        }

        final UUID requestedHospitalId;

        try {
            requestedHospitalId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException(
                    "Invalid doctor presence topic"
            );
        }

        Object storedHospital = accessor.getSessionAttributes() == null
                ? null
                : accessor.getSessionAttributes()
                        .get(WS_HOSPITAL_ATTRIBUTE);

        if (!(storedHospital instanceof String storedHospitalValue)
                || storedHospitalValue.isBlank()) {

            throw new AccessDeniedException(
                    "Hospital context is required"
            );
        }

        final UUID authenticatedHospitalId;

        try {
            authenticatedHospitalId = UUID.fromString(
                    storedHospitalValue
            );
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException(
                    "Invalid WebSocket hospital context"
            );
        }

        if (!authenticatedHospitalId.equals(requestedHospitalId)) {
            throw new AccessDeniedException(
                    "You are not authorized to subscribe to this hospital's doctor presence"
            );
        }
    }

    private void validateQueueSubscription(
            StompHeaderAccessor accessor) {

        String destination = accessor.getDestination();

        if (destination == null
                || !destination.startsWith("/topic/hospital/")) {
            return;
        }

        Matcher matcher = QUEUE_TOPIC_PATTERN.matcher(destination);

        if (!matcher.matches()) {
            if (destination.contains("/queue")) {
                throw new AccessDeniedException(
                        "Invalid queue topic"
                );
            }
            return;
        }

        final UUID requestedHospitalId;

        try {
            requestedHospitalId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException(
                    "Invalid queue topic"
            );
        }

        Object storedHospital = accessor.getSessionAttributes() == null
                ? null
                : accessor.getSessionAttributes()
                        .get(WS_HOSPITAL_ATTRIBUTE);

        if (!(storedHospital instanceof String storedHospitalValue)
                || storedHospitalValue.isBlank()) {

            throw new AccessDeniedException(
                    "Hospital context is required"
            );
        }

        final UUID authenticatedHospitalId;

        try {
            authenticatedHospitalId = UUID.fromString(
                    storedHospitalValue
            );
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException(
                    "Invalid WebSocket hospital context"
            );
        }

        if (!authenticatedHospitalId.equals(requestedHospitalId)) {
            throw new AccessDeniedException(
                    "You are not authorized to subscribe to this hospital's queue"
            );
        }
    }

    private void blockServerOnlyPublishing(
            StompHeaderAccessor accessor) {

        String destination = accessor.getDestination();

        if (destination == null) {
            return;
        }

        if (destination.startsWith(NOTIFICATION_TOPIC_PREFIX)) {
            throw new AccessDeniedException(
                    "Client notification publishing is not allowed"
            );
        }

        if (destination.startsWith("/topic/hospital/")
                && destination.contains("/doctor-presence")) {

            throw new AccessDeniedException(
                    "Client doctor presence publishing is not allowed"
            );
        }

        if (destination.startsWith("/topic/hospital/")
                && destination.endsWith("/queue")) {

            throw new AccessDeniedException(
                    "Client queue publishing is not allowed"
            );
        }
    }

    private Authentication getAuthentication(
            StompHeaderAccessor accessor) {

        Principal principal = accessor.getUser();

        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication required"
            );
        }

        if (!(principal instanceof Authentication authentication)) {
            throw new AccessDeniedException(
                    "Authentication required"
            );
        }

        return authentication;
    }

    private UserEntity getCurrentUser(
            Authentication authentication) {

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserEntity currentUser)) {
            throw new AccessDeniedException(
                    "Authenticated user identity is unavailable"
            );
        }

        return currentUser;
    }
}
