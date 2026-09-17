package org.proj.controller;

import jakarta.validation.Valid;
import org.proj.dto.QueueRequest;
import org.proj.dto.QueueResponse;
import org.proj.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    @Autowired
    private QueueService queueService;

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<QueueResponse> checkIn(
            @Valid @RequestBody QueueRequest request) {
        QueueResponse response = queueService.checkIn(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<List<QueueResponse>> getTodayQueue() {
        return ResponseEntity.ok(queueService.getTodayQueue());
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST')")
    public ResponseEntity<List<QueueResponse>> getTodayQueueByDoctor(
            @PathVariable UUID doctorId) {
        return ResponseEntity.ok(queueService.getTodayQueueByDoctor(doctorId));
    }

    @PutMapping("/call-next")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<QueueResponse> callNext(
            @RequestParam UUID doctorId) {
        return ResponseEntity.ok(queueService.callNext(doctorId));
    }

    @PutMapping("/start/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<QueueResponse> startConsultation(
            @PathVariable UUID id) {
        return ResponseEntity.ok(queueService.startConsultation(id));
    }

    @PutMapping("/complete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<QueueResponse> completeConsultation(
            @PathVariable UUID id) {
        return ResponseEntity.ok(queueService.completeConsultation(id));
    }

    @PutMapping("/skip/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<QueueResponse> skipQueue(
            @PathVariable UUID id) {
        return ResponseEntity.ok(queueService.skipQueue(id));
    }

    @PutMapping("/recall/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<QueueResponse> recallSkippedPatient(
            @PathVariable UUID id) {
        return ResponseEntity.ok(queueService.recallSkippedPatient(id));
    }
}
