package org.proj.controller;

import jakarta.validation.Valid;
import org.proj.dto.GeneralEnquiryRequest;
import org.proj.dto.GeneralEnquiryResponse;
import org.proj.service.GeneralEnquiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/enquiries")
public class GeneralEnquiryController {

    @Autowired
    private GeneralEnquiryService generalEnquiryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<GeneralEnquiryResponse> createEnquiry(@Valid @RequestBody GeneralEnquiryRequest request) {
        return new ResponseEntity<>(generalEnquiryService.createEnquiry(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<List<GeneralEnquiryResponse>> getHospitalEnquiries() {
        return ResponseEntity.ok(generalEnquiryService.getHospitalEnquiries());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<GeneralEnquiryResponse> updateEnquiryStatus(@PathVariable UUID id, @Valid @RequestBody GeneralEnquiryRequest request) {
        return ResponseEntity.ok(generalEnquiryService.updateEnquiryStatus(id, request));
    }
}
