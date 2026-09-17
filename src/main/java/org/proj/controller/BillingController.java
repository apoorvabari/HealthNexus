package org.proj.controller;

import jakarta.validation.Valid;

import org.proj.dto.BillingRequest;
import org.proj.dto.BillingResponse;
import org.proj.service.BillingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @Autowired
    private BillingService billingService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<?> generateInvoice(
            @Valid @RequestBody BillingRequest request) {

        try {

            BillingResponse response =
                    billingService.generateInvoice(request);

            return new ResponseEntity<>(
                    response,
                    HttpStatus.CREATED
            );

        } catch (IllegalArgumentException e) {

            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (AccessDeniedException e) {

            return buildErrorResponse(
                    HttpStatus.FORBIDDEN,
                    e.getMessage()
            );

        } catch (Exception e) {

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while generating invoice"
            );
        }
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'PATIENT')")
    public ResponseEntity<?> processPayment(
            @PathVariable UUID id,
            @RequestBody(required = false)
            Map<String, String> body) {

        try {

            String paymentMethod =
                    body != null
                            ? body.get("paymentMethod")
                            : null;

            BillingResponse response =
                    billingService.processPayment(
                            id,
                            paymentMethod
                    );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (AccessDeniedException e) {

            return buildErrorResponse(
                    HttpStatus.FORBIDDEN,
                    e.getMessage()
            );

        } catch (Exception e) {

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while processing payment"
            );
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT', 'RECEPTIONIST')")
    public ResponseEntity<?> getBillingById(
            @PathVariable UUID id) {

        try {

            return ResponseEntity.ok(
                    billingService.getBillingById(id)
            );

        } catch (IllegalArgumentException e) {

            return buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (AccessDeniedException e) {

            return buildErrorResponse(
                    HttpStatus.FORBIDDEN,
                    e.getMessage()
            );

        } catch (Exception e) {

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while fetching billing"
            );
        }
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT', 'RECEPTIONIST')")
    public ResponseEntity<?> getBillingByAppointmentId(
            @PathVariable UUID appointmentId) {

        try {

            return ResponseEntity.ok(
                    billingService
                            .getBillingByAppointmentId(
                                    appointmentId
                            )
            );

        } catch (IllegalArgumentException e) {

            return buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (AccessDeniedException e) {

            return buildErrorResponse(
                    HttpStatus.FORBIDDEN,
                    e.getMessage()
            );

        } catch (Exception e) {

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while fetching invoice for appointment"
            );
        }
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT', 'RECEPTIONIST')")
    public ResponseEntity<?> getBillingByPatientId(
            @PathVariable UUID patientId) {

        try {

            return ResponseEntity.ok(
                    billingService
                            .getBillingByPatientId(patientId)
            );

        } catch (IllegalArgumentException e) {

            return buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (AccessDeniedException e) {

            return buildErrorResponse(
                    HttpStatus.FORBIDDEN,
                    e.getMessage()
            );

        } catch (Exception e) {

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while fetching patient invoices"
            );
        }
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT', 'RECEPTIONIST')")
    public ResponseEntity<?> downloadReceipt(
            @PathVariable UUID id) {

        try {

            byte[] pdf =
                    billingService.generateReceiptPdf(id);

            return ResponseEntity.ok()

                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=healthnexus-receipt-"
                                    + id
                                    + ".pdf"
                    )

                    .contentType(
                            MediaType.APPLICATION_PDF
                    )

                    .body(pdf);

        } catch (IllegalArgumentException e) {

            return buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (AccessDeniedException e) {

            return buildErrorResponse(
                    HttpStatus.FORBIDDEN,
                    e.getMessage()
            );

        } catch (Exception e) {

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while generating receipt"
            );
        }
    }

    private ResponseEntity<Map<String, String>>
    buildErrorResponse(
            HttpStatus status,
            String message) {

        Map<String, String> errorResponse =
                new HashMap<>();

        errorResponse.put(
                "message",
                message
        );

        return new ResponseEntity<>(
                errorResponse,
                status
        );
    }
}
