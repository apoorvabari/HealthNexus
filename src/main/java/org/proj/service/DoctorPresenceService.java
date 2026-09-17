package org.proj.service;

import org.proj.entity.DoctorEntity;
import org.proj.entity.UserEntity;
import org.proj.repository.DoctorRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DoctorPresenceService {

    private final Map<UUID, Set<UUID>> hospitalToOnlineDoctors =
            new ConcurrentHashMap<>();

    private final Map<String, UUID> sessionToDoctorMap =
            new ConcurrentHashMap<>();

    private final Map<String, UUID> sessionToHospitalMap =
            new ConcurrentHashMap<>();

    private final Map<UUID, Set<String>> doctorToSessions =
            new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DoctorRepo doctorRepo;

    @EventListener
    public void handleWebSocketConnectListener(
            SessionConnectedEvent event) {

        SimpMessageHeaderAccessor headers =
                SimpMessageHeaderAccessor.wrap(event.getMessage());

        String sessionId = headers.getSessionId();

        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        if (!(event.getUser()
                instanceof UsernamePasswordAuthenticationToken auth)) {
            return;
        }

        if (!(auth.getPrincipal() instanceof UserEntity user)) {
            return;
        }

        if (user.getRole() == null
                || user.getRole().getRoleName() == null
                || !user.getRole()
                        .getRoleName()
                        .equalsIgnoreCase("DOCTOR")) {
            return;
        }

        UUID accountId = user.getId();

        if (accountId == null) {
            return;
        }

        Optional<DoctorEntity> doctorOpt =
                doctorRepo.findByAccountId(accountId);

        if (doctorOpt.isEmpty()) {
            return;
        }

        DoctorEntity doctor = doctorOpt.get();

        if (doctor.getId() == null
                || doctor.getHospital() == null
                || doctor.getHospital().getId() == null) {
            return;
        }

        UUID doctorId = doctor.getId();
        UUID hospitalId = doctor.getHospital().getId();

        Set<String> sessions =
                doctorToSessions.computeIfAbsent(
                        doctorId,
                        key -> ConcurrentHashMap.newKeySet()
                );

        boolean isFirstSession = sessions.isEmpty();

        sessions.add(sessionId);

        sessionToDoctorMap.put(sessionId, doctorId);
        sessionToHospitalMap.put(sessionId, hospitalId);

        if (isFirstSession) {

            hospitalToOnlineDoctors
                    .computeIfAbsent(
                            hospitalId,
                            key -> ConcurrentHashMap.newKeySet()
                    )
                    .add(doctorId);

            broadcastPresence(
                    hospitalId,
                    doctorId,
                    true
            );
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(
            SessionDisconnectEvent event) {

        String sessionId = event.getSessionId();

        if (sessionId == null) {
            return;
        }

        UUID doctorId =
                sessionToDoctorMap.remove(sessionId);

        UUID hospitalId =
                sessionToHospitalMap.remove(sessionId);

        if (doctorId == null || hospitalId == null) {
            return;
        }

        Set<String> sessions =
                doctorToSessions.get(doctorId);

        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);

        if (!sessions.isEmpty()) {
            return;
        }

        doctorToSessions.remove(
                doctorId,
                sessions
        );

        Set<UUID> onlineDoctors =
                hospitalToOnlineDoctors.get(hospitalId);

        if (onlineDoctors != null) {

            onlineDoctors.remove(doctorId);

            if (onlineDoctors.isEmpty()) {
                hospitalToOnlineDoctors.remove(
                        hospitalId,
                        onlineDoctors
                );
            }
        }

        broadcastPresence(
                hospitalId,
                doctorId,
                false
        );
    }

    private void broadcastPresence(
            UUID hospitalId,
            UUID doctorId,
            boolean online) {

        Map<String, Object> payload =
                Map.of(
                        "doctorId", doctorId,
                        "online", online
                );

        messagingTemplate.convertAndSend(
                "/topic/hospital/"
                        + hospitalId
                        + "/doctor-presence",
                payload
        );
    }

    public Set<UUID> getOnlineDoctors(UUID hospitalId) {

        if (hospitalId == null) {
            return Collections.emptySet();
        }

        Set<UUID> onlineDoctors =
                hospitalToOnlineDoctors.get(hospitalId);

        if (onlineDoctors == null
                || onlineDoctors.isEmpty()) {
            return Collections.emptySet();
        }

        return Set.copyOf(onlineDoctors);
    }
}
