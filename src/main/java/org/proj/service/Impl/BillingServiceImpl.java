package org.proj.service.impl;

import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfWriter;

import org.proj.dto.BillingRequest;
import org.proj.dto.BillingResponse;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.BillingEntity;
import org.proj.entity.PatientEntity;
import org.proj.entity.NotificationEntity.NotificationType;
import org.proj.repository.BillingRepo;
import org.proj.service.AppointmentService;
import org.proj.service.BillingService;
import org.proj.service.NotificationService;
import org.proj.service.PatientService;
import org.proj.security.SecurityUtils;
import org.proj.entity.DoctorEntity;
import org.proj.entity.UserEntity;
import org.proj.service.TenantContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final BillingRepo billingRepo;
    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final PatientService patientService;
    private final TenantContextService tenantContextService;

    // =========================================================
    // GENERATE INVOICE
    // =========================================================

    @Override
    @Transactional
    public BillingResponse generateInvoice(BillingRequest request) {
        try {
            BillingRequest validReq = Optional.ofNullable(request)
                    .orElseThrow(() -> new IllegalArgumentException("Billing request is required"));

            UUID appointmentId = Optional.ofNullable(validReq.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Appointment ID is required"));

            AppointmentEntity appointment = Optional.ofNullable(appointmentService.findAppointmentById(appointmentId))
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            // Security: Only ADMIN or the doctor assigned to this appointment can generate the invoice.
            validateInvoiceGenerationAccess(appointment);

            Optional.ofNullable(appointment.getAppointmentStatus())
                    .filter(status -> status == AppointmentEntity.AppointmentStatus.COMPLETED)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice can only be generated for a completed appointment"));

            Optional.of(billingRepo.existsByAppointmentId(appointment.getId()))
                    .filter(exists -> !exists)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice already exists for this appointment"));

            BigDecimal fee = (appointment.getDoctor() != null && appointment.getDoctor().getConsultationFee() != null)
                    ? appointment.getDoctor().getConsultationFee()
                    : BigDecimal.ZERO;

            BillingEntity billing = BillingEntity.builder()
                    .appointment(appointment)
                    .patient(appointment.getPatient())
                    .amount(fee)
                    .invoiceNumber("INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase())
                    .paymentStatus(BillingEntity.PaymentStatus.PENDING)
                    .build();

            BillingEntity savedBilling = billingRepo.save(billing);

            if (appointment.getPatient() != null && appointment.getPatient().getAccount() != null) {
                notificationService.createNotification(
                        appointment.getPatient().getAccount(),
                        "Invoice Generated",
                        "A new invoice has been generated for your consultation.",
                        NotificationType.INVOICE_GENERATED
                );
            }

            return mapToResponse(savedBilling);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to generate invoice";
            throw new RuntimeException(msg, e);
        }
    }

    // =========================================================
    // PROCESS PAYMENT
    // =========================================================

    @Override
    @Transactional
    public BillingResponse processPayment(UUID billingId, String paymentMethod) {
        try {
            UUID validBillingId = Optional.ofNullable(billingId)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice ID is required"));

            UUID currentHospitalId = requireCurrentHospital();

            BillingEntity billing = billingRepo.findByIdAndAppointmentHospitalId(validBillingId, currentHospitalId)
                    .orElseThrow(() -> billingRepo.existsById(validBillingId)
                            ? new AccessDeniedException("Access denied")
                            : new IllegalArgumentException("Invoice not found"));

            validateBillingReadAccess(billing);

            Optional.ofNullable(billing.getPaymentStatus())
                    .filter(status -> status != BillingEntity.PaymentStatus.PAID)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice is already paid"));

            Optional.ofNullable(billing.getPaymentStatus())
                    .filter(status -> status != BillingEntity.PaymentStatus.CANCELLED)
                    .orElseThrow(() -> new IllegalArgumentException("Cancelled invoice cannot be paid"));

            if (paymentMethod == null || paymentMethod.trim().isBlank()) {
                throw new IllegalArgumentException("Payment method is required");
            }
            String validPaymentMethodStr = paymentMethod.trim();

            final BillingEntity.PaymentMethod method;
            try {
                method = BillingEntity.PaymentMethod.valueOf(validPaymentMethodStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid payment method. Allowed values: CASH, CARD, ONLINE");
            }

            billing.setPaymentStatus(BillingEntity.PaymentStatus.PAID);
            billing.setPaymentMethod(method);
            billing.setPaymentDate(LocalDateTime.now());

            BillingEntity savedBilling = billingRepo.save(billing);
            return mapToResponse(savedBilling);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to process payment";
            throw new RuntimeException(msg, e);
        }
    }

    // =========================================================
    // GET BILLING BY ID
    // =========================================================

    @Override
    public BillingResponse getBillingById(UUID id) {
        try {
            UUID validId = Optional.ofNullable(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice ID is required"));

            UUID currentHospitalId = requireCurrentHospital();

            BillingEntity billing = billingRepo.findByIdAndAppointmentHospitalId(validId, currentHospitalId)
                    .orElseThrow(() -> billingRepo.existsById(validId)
                            ? new AccessDeniedException("Access denied")
                            : new IllegalArgumentException("Invoice not found"));

            validateBillingReadAccess(billing);
            return mapToResponse(billing);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve invoice";
            throw new RuntimeException(msg, e);
        }
    }

    // =========================================================
    // GET BILLING BY APPOINTMENT
    // =========================================================

    @Override
    public BillingResponse getBillingByAppointmentId(UUID appointmentId) {
        try {
            UUID validApptId = Optional.ofNullable(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment ID is required"));

            UUID currentHospitalId = requireCurrentHospital();

            BillingEntity billing = billingRepo.findByAppointmentIdAndAppointmentHospitalId(validApptId, currentHospitalId)
                    .orElseThrow(() -> billingRepo.existsByAppointmentId(validApptId)
                            ? new AccessDeniedException("Access denied")
                            : new IllegalArgumentException("Invoice not found for this appointment"));

            validateBillingReadAccess(billing);
            return mapToResponse(billing);
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve invoice for appointment";
            throw new RuntimeException(msg, e);
        }
    }

    // =========================================================
    // GET PATIENT BILLING HISTORY
    // =========================================================

    @Override
    public List<BillingResponse> getBillingByPatientId(UUID patientId) {
        try {
            UUID validPatientId = Optional.ofNullable(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient ID is required"));

            boolean admin = hasRole("ROLE_ADMIN");
            boolean doctor = hasRole("ROLE_DOCTOR");
            boolean receptionist = hasRole("ROLE_RECEPTIONIST");
            boolean patient = hasRole("ROLE_PATIENT");

            if (patient) {
                UUID currentAccountId = SecurityUtils.getCurrentUser().getId();
                PatientEntity patientEntity = patientService.findPatientById(validPatientId);

                UUID patientAccId = (patientEntity != null && patientEntity.getAccount() != null)
                        ? patientEntity.getAccount().getId()
                        : null;

                if (patientAccId == null || !patientAccId.equals(currentAccountId)) {
                    throw new AccessDeniedException("You are not authorized to view these invoices");
                }

                UUID hospitalId = requireCurrentHospital();
                return billingRepo.findByPatientIdAndAppointmentHospitalIdOrderByBillingDateDesc(validPatientId, hospitalId)
                        .stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }

            if (admin || receptionist) {
                UUID hospitalId = requireCurrentHospital();
                return billingRepo.findByPatientIdAndAppointmentHospitalIdOrderByBillingDateDesc(validPatientId, hospitalId)
                        .stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }

            if (doctor) {
                UUID currentAccountId = SecurityUtils.getCurrentUser().getId();
                UUID doctorHospitalId = requireCurrentHospital();

                return billingRepo.findByPatientIdAndDoctorAccountIdAndHospitalId(validPatientId, currentAccountId, doctorHospitalId)
                        .stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }

            throw new AccessDeniedException("You are not authorized to view these invoices");
        } catch (AccessDeniedException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to retrieve patient billing history";
            throw new RuntimeException(msg, e);
        }
    }

    // =========================================================
    // GENERATE RECEIPT PDF
    // =========================================================

    @Override
    public byte[] generateReceiptPdf(UUID billingId) {
        try {
            UUID validBillingId = Optional.ofNullable(billingId)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice ID is required"));

            UUID currentHospitalId = requireCurrentHospital();

            BillingEntity billing = billingRepo.findByIdAndAppointmentHospitalId(validBillingId, currentHospitalId)
                    .orElseThrow(() -> billingRepo.existsById(validBillingId)
                            ? new AccessDeniedException("Access denied")
                            : new IllegalArgumentException("Invoice not found"));

            validateBillingReadAccess(billing);

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                Document document = new Document();
                PdfWriter.getInstance(document, output);
                document.open();

                UserEntity patAcc = (billing.getPatient() != null) ? billing.getPatient().getAccount() : null;
                String patientName = (patAcc != null) ? fullName(patAcc.getFirstName(), patAcc.getLastName()) : "";

                DoctorEntity doc = (billing.getAppointment() != null) ? billing.getAppointment().getDoctor() : null;
                UserEntity docAcc = (doc != null) ? doc.getAccount() : null;
                String doctorName = (docAcc != null) ? fullName(docAcc.getFirstName(), docAcc.getLastName()) : "";

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

                document.add(new Paragraph("HEALTHNEXUS"));
                document.add(new Paragraph("PAYMENT RECEIPT"));
                document.add(new Paragraph(" "));

                Table table = new Table(2);
                table.setWidth(100);

                addRow(table, "Invoice Number", billing.getInvoiceNumber());
                addRow(table, "Patient", patientName);
                addRow(table, "Doctor", doctorName);
                addRow(table, "Appointment ID", (billing.getAppointment() != null && billing.getAppointment().getId() != null) ? billing.getAppointment().getId().toString() : "-");
                addRow(table, "Billing Date", (billing.getBillingDate() != null) ? billing.getBillingDate().format(formatter) : "-");
                addRow(table, "Amount", "INR " + billing.getAmount());
                addRow(table, "Payment Status", (billing.getPaymentStatus() != null) ? billing.getPaymentStatus().name() : "-");
                addRow(table, "Payment Method", (billing.getPaymentMethod() != null) ? billing.getPaymentMethod().name() : "-");
                addRow(table, "Payment Date", (billing.getPaymentDate() != null) ? billing.getPaymentDate().format(formatter) : "-");

                document.add(table);
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Thank you for using HealthNexus."));

                document.close();
                return output.toByteArray();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to generate receipt PDF", e);
            }
        } catch (AccessDeniedException | IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Unable to generate receipt PDF";
            throw new RuntimeException(msg, e);
        }
    }

    private void validateInvoiceGenerationAccess(AppointmentEntity appointment) {
        if (appointment == null || appointment.getHospital() == null) {
            throw new AccessDeniedException("Appointment or hospital details incomplete");
        }

        UUID resourceHospitalId = appointment.getHospital().getId();

        if (hasRole("ROLE_ADMIN")) {
            UUID currentHospitalId = requireCurrentHospital();
            if (!resourceHospitalId.equals(currentHospitalId)) {
                throw new AccessDeniedException("You can only generate invoices for your own hospital");
            }
            return;
        }

        if (!hasRole("ROLE_DOCTOR")) {
            throw new AccessDeniedException("Only an admin or the assigned doctor can generate an invoice");
        }

        UUID currentAccountId = SecurityUtils.getCurrentUser().getId();

        UserEntity docAcc = (appointment.getDoctor() != null) ? appointment.getDoctor().getAccount() : null;
        UUID docAccountId = (docAcc != null) ? docAcc.getId() : null;
        if (docAccountId == null || !docAccountId.equals(currentAccountId)) {
            throw new AccessDeniedException("You can only generate invoices for your own appointments");
        }

        UUID doctorHospitalId = requireCurrentHospital();
        if (!resourceHospitalId.equals(doctorHospitalId)) {
            throw new AccessDeniedException("You are not authorized to generate invoices for another hospital");
        }
    }

    private void validateBillingReadAccess(BillingEntity billing) {
        if (billing == null || billing.getAppointment() == null || billing.getAppointment().getHospital() == null) {
            throw new AccessDeniedException("Billing information is incomplete");
        }

        UUID resourceHospitalId = billing.getAppointment().getHospital().getId();

        if (hasRole("ROLE_ADMIN") || hasRole("ROLE_RECEPTIONIST")) {
            UUID currentHospitalId = requireCurrentHospital();
            if (!resourceHospitalId.equals(currentHospitalId)) {
                throw new AccessDeniedException("You are not authorized to access billing from another hospital");
            }
            return;
        }

        if (hasRole("ROLE_DOCTOR")) {
            UUID currentAccountId = SecurityUtils.getCurrentUser().getId();

            DoctorEntity doc = (billing.getAppointment() != null) ? billing.getAppointment().getDoctor() : null;
            UserEntity docAcc = (doc != null) ? doc.getAccount() : null;
            UUID docAccountId = (docAcc != null) ? docAcc.getId() : null;
            if (docAccountId == null || !docAccountId.equals(currentAccountId)) {
                throw new AccessDeniedException("You are not authorized to view this invoice");
            }

            UUID doctorHospitalId = requireCurrentHospital();
            if (!resourceHospitalId.equals(doctorHospitalId)) {
                throw new AccessDeniedException("You are not authorized to access billing from another hospital");
            }
            return;
        }

        if (hasRole("ROLE_PATIENT")) {
            UUID currentAccountId = SecurityUtils.getCurrentUser().getId();

            UserEntity patAcc = (billing.getPatient() != null) ? billing.getPatient().getAccount() : null;
            UUID patAccountId = (patAcc != null) ? patAcc.getId() : null;
            if (patAccountId == null || !patAccountId.equals(currentAccountId)) {
                throw new AccessDeniedException("You are not authorized to view this invoice");
            }
            return;
        }

        throw new AccessDeniedException("You are not authorized to access this invoice");
    }

    private UUID requireCurrentHospital() {
        UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
        if (hospitalId == null) {
            throw new AccessDeniedException("You are not assigned to any hospital");
        }
        return hospitalId;
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }

    private void addRow(Table table, String key, String value) {
        table.addCell(new Cell(new Paragraph(key)));
        table.addCell(new Cell(new Paragraph(value == null ? "-" : value)));
    }

    private String fullName(String first, String last) {
        return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
    }

    private BillingResponse mapToResponse(BillingEntity entity) {
        UserEntity patAcc = (entity.getPatient() != null) ? entity.getPatient().getAccount() : null;
        String patientName = (patAcc != null) ? fullName(patAcc.getFirstName(), patAcc.getLastName()) : "";

        DoctorEntity doc = (entity.getAppointment() != null) ? entity.getAppointment().getDoctor() : null;
        UserEntity docAcc = (doc != null) ? doc.getAccount() : null;
        String doctorName = (docAcc != null) ? fullName(docAcc.getFirstName(), docAcc.getLastName()) : "";

        return BillingResponse.builder()
                .id(entity.getId())
                .appointmentId(entity.getAppointment() != null ? entity.getAppointment().getId() : null)
                .patientId(entity.getPatient() != null ? entity.getPatient().getId() : null)
                .patientName(patientName)
                .doctorName(doctorName)
                .amount(entity.getAmount())
                .paymentStatus(entity.getPaymentStatus() != null ? entity.getPaymentStatus().name() : null)
                .paymentMethod(entity.getPaymentMethod() != null ? entity.getPaymentMethod().name() : null)
                .invoiceNumber(entity.getInvoiceNumber())
                .billingDate(entity.getBillingDate())
                .paymentDate(entity.getPaymentDate())
                .build();
    }
}
