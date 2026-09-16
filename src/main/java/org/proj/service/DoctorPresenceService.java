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

    /*
     * Hospital ID
     *      ↓
     * Set of doctor IDs who are currently online
     */
    private final Map<UUID, Set<UUID>> hospitalToOnlineDoctors =
            new ConcurrentHashMap<>();

    /*
     * WebSocket session ID
     *      ↓
     * Doctor ID
     */
    private final Map<String, UUID> sessionToDoctorMap =
            new ConcurrentHashMap<>();

    /*
     * WebSocket session ID
     *      ↓
     * Hospital ID
     */
    private final Map<String, UUID> sessionToHospitalMap =
            new ConcurrentHashMap<>();

    /*
     * Doctor ID
     *      ↓
     * All active WebSocket sessions for that doctor
     *
     * This is required because one doctor can have:
     * - browser session
     * - mobile session
     * - another browser tab
     */
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

        /*
         * Get the existing sessions for this doctor.
         */
        Set<String> sessions =
                doctorToSessions.computeIfAbsent(
                        doctorId,
                        key -> ConcurrentHashMap.newKeySet()
                );

        /*
         * If there are no sessions before this connection,
         * this is the transition:
         *
         * OFFLINE → ONLINE
         */
        boolean isFirstSession = sessions.isEmpty();

        sessions.add(sessionId);

        /*
         * Store the relationship between:
         *
         * session → doctor
         * session → hospital
         */
        sessionToDoctorMap.put(sessionId, doctorId);
        sessionToHospitalMap.put(sessionId, hospitalId);

        /*
         * Broadcast ONLINE only once.
         */
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

        /*
         * We only process sessions that were registered
         * as doctor sessions.
         */
        if (doctorId == null || hospitalId == null) {
            return;
        }

        Set<String> sessions =
                doctorToSessions.get(doctorId);

        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);

        /*
         * Doctor still has another active session.
         *
         * Example:
         *
         * Browser 1 → disconnected
         * Browser 2 → still connected
         *
         * Therefore doctor remains ONLINE.
         */
        if (!sessions.isEmpty()) {
            return;
        }

        /*
         * No sessions remain.
         *
         * Therefore:
         *
         * ONLINE → OFFLINE
         */
        doctorToSessions.remove(
                doctorId,
                sessions
        );

        Set<UUID> onlineDoctors =
                hospitalToOnlineDoctors.get(hospitalId);

        if (onlineDoctors != null) {

            onlineDoctors.remove(doctorId);

            /*
             * Remove empty hospital entries.
             */
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

        /*
         * Return a snapshot rather than exposing the
         * internal mutable ConcurrentHashMap set.
         */
        return Set.copyOf(onlineDoctors);
    }
}