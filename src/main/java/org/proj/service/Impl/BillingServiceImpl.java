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

        AppointmentEntity appointment =
                appointmentService.findAppointmentById(request.getAppointmentId());

        // Security:
        // Only ADMIN or the doctor assigned to this appointment
        // can generate the invoice.
        validateInvoiceGenerationAccess(appointment);

        // Invoice should only be generated after consultation/
        // appointment is completed.
        if (appointment.getAppointmentStatus()
                != AppointmentEntity.AppointmentStatus.COMPLETED) {

            throw new IllegalArgumentException(
                    "Invoice can only be generated for a completed appointment"
            );
        }

        if (billingRepo.existsByAppointmentId(appointment.getId())) {

            throw new IllegalArgumentException(
                    "Invoice already exists for this appointment"
            );
        }

        BigDecimal fee = BigDecimal.ZERO;

        if (appointment.getDoctor() != null
                && appointment.getDoctor().getConsultationFee() != null) {

            fee = appointment.getDoctor().getConsultationFee();
        }

        BillingEntity billing = BillingEntity.builder()
                .appointment(appointment)
                .patient(appointment.getPatient())
                .amount(fee)
                .invoiceNumber(
                        "INV-" +
                        UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 12)
                                .toUpperCase()
                )
                .paymentStatus(BillingEntity.PaymentStatus.PENDING)
                .build();

        BillingEntity savedBilling = billingRepo.save(billing);

        // Notify patient that invoice has been generated.
        notificationService.createNotification(
                appointment.getPatient().getAccount(),
                "Invoice Generated",
                "A new invoice has been generated for your consultation.",
                NotificationType.INVOICE_GENERATED
        );

        return mapToResponse(savedBilling);
    }


    // =========================================================
    // PROCESS PAYMENT
    // =========================================================

    @Override
    @Transactional
    public BillingResponse processPayment(
            UUID billingId,
            String paymentMethod) {

        UUID currentHospitalId = tenantContextService.getCurrentUserHospitalId();
        if (currentHospitalId == null) {
            throw new AccessDeniedException("You are not assigned to any hospital");
        }

        BillingEntity billing = billingRepo.findByIdAndAppointmentHospitalId(billingId, currentHospitalId)
                .orElseThrow(() -> {
                    if (billingRepo.existsById(billingId)) {
                        return new AccessDeniedException("Access denied");
                    }
                    return new IllegalArgumentException("Invoice not found");
                });

        // Enforce Hospital isolation and Ownership
        validateBillingReadAccess(billing);

        // Prevent duplicate payment.
        if (billing.getPaymentStatus()
                == BillingEntity.PaymentStatus.PAID) {

            throw new IllegalArgumentException(
                    "Invoice is already paid"
            );
        }

        // Cancelled invoices cannot be paid.
        if (billing.getPaymentStatus()
                == BillingEntity.PaymentStatus.CANCELLED) {

            throw new IllegalArgumentException(
                    "Cancelled invoice cannot be paid"
            );
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {

            throw new IllegalArgumentException(
                    "Payment method is required"
            );
        }

        final BillingEntity.PaymentMethod method;

        try {

            method = BillingEntity.PaymentMethod.valueOf(
                    paymentMethod.trim().toUpperCase()
            );

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid payment method. Allowed values: CASH, CARD, ONLINE"
            );
        }

        billing.setPaymentStatus(
                BillingEntity.PaymentStatus.PAID
        );

        billing.setPaymentMethod(method);

        billing.setPaymentDate(LocalDateTime.now());

        BillingEntity savedBilling = billingRepo.save(billing);

        return mapToResponse(savedBilling);
    }


    // =========================================================
    // GET BILLING BY ID
    // =========================================================

    @Override
    public BillingResponse getBillingById(UUID id) {

        UUID currentHospitalId = tenantContextService.getCurrentUserHospitalId();
        if (currentHospitalId == null) {
            throw new AccessDeniedException("You are not assigned to any hospital");
        }

        BillingEntity billing = billingRepo.findByIdAndAppointmentHospitalId(id, currentHospitalId)
                .orElseThrow(() -> {
                    if (billingRepo.existsById(id)) {
                        return new AccessDeniedException("Access denied");
                    }
                    return new IllegalArgumentException("Invoice not found");
                });

        validateBillingReadAccess(billing);

        return mapToResponse(billing);
    }


    // =========================================================
    // GET BILLING BY APPOINTMENT
    // =========================================================

    @Override
    public BillingResponse getBillingByAppointmentId(
            UUID appointmentId) {

        UUID currentHospitalId = tenantContextService.getCurrentUserHospitalId();
        if (currentHospitalId == null) {
            throw new AccessDeniedException("You are not assigned to any hospital");
        }

        BillingEntity billing =
                billingRepo.findByAppointmentIdAndAppointmentHospitalId(appointmentId, currentHospitalId)
                        .orElseThrow(() -> {
                            if (billingRepo.existsByAppointmentId(appointmentId)) {
                                return new AccessDeniedException("Access denied");
                            }
                            return new IllegalArgumentException("Invoice not found for this appointment");
                        });

        validateBillingReadAccess(billing);

        return mapToResponse(billing);
    }


    // =========================================================
    // GET PATIENT BILLING HISTORY
    // =========================================================

    @Override
    public List<BillingResponse> getBillingByPatientId(
            UUID patientId) {

        boolean admin = hasRole("ROLE_ADMIN");
        boolean doctor = hasRole("ROLE_DOCTOR");
        boolean receptionist = hasRole("ROLE_RECEPTIONIST");
        boolean patient = hasRole("ROLE_PATIENT");

        /*
         * =========================================================
         * PATIENT
         * =========================================================
         * Resolve patient ownership via account ID before querying.
         */
        if (patient) {
            UUID currentAccountId = SecurityUtils.getCurrentUser().getId();
            PatientEntity patientEntity = patientService.findPatientById(patientId);

            if (patientEntity == null || patientEntity.getAccount() == null ||
                    !currentAccountId.equals(patientEntity.getAccount().getId())) {
                throw new AccessDeniedException(
                        "You are not authorized to view these invoices"
                );
            }

            UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
            if (hospitalId == null) {
                throw new AccessDeniedException("You are not assigned to any hospital");
            }
            List<BillingEntity> billings = billingRepo.findByPatientIdAndAppointmentHospitalIdOrderByBillingDateDesc(patientId, hospitalId);

            return billings.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        /*
         * =========================================================
         * ADMIN / RECEPTIONIST
         * =========================================================
         */
        if (admin || receptionist) {
            UUID hospitalId = tenantContextService.getCurrentUserHospitalId();
            if (hospitalId == null) {
                throw new AccessDeniedException("You are not assigned to any hospital");
            }
            List<BillingEntity> billings = billingRepo.findByPatientIdAndAppointmentHospitalIdOrderByBillingDateDesc(patientId, hospitalId);
            return billings.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        /*
         * =========================================================
         * DOCTOR
         * =========================================================
         */
        if (doctor) {
            UUID currentAccountId = SecurityUtils.getCurrentUser().getId();
            UUID doctorHospitalId = tenantContextService.getCurrentUserHospitalId();

            if (doctorHospitalId == null) {
                throw new AccessDeniedException("You are not assigned to any hospital");
            }

            List<BillingEntity> billings = billingRepo.findByPatientIdAndDoctorAccountIdAndHospitalId(
                    patientId,
                    currentAccountId,
                    doctorHospitalId
            );

            return billings.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        throw new AccessDeniedException("You are not authorized to view these invoices");
    }


    // =========================================================
    // GENERATE RECEIPT PDF
    // =========================================================

    @Override
    public byte[] generateReceiptPdf(UUID billingId) {

        UUID currentHospitalId = tenantContextService.getCurrentUserHospitalId();
        if (currentHospitalId == null) {
            throw new AccessDeniedException("You are not assigned to any hospital");
        }

        BillingEntity billing = billingRepo.findByIdAndAppointmentHospitalId(billingId, currentHospitalId)
                .orElseThrow(() -> {
                    if (billingRepo.existsById(billingId)) {
                        return new AccessDeniedException("Access denied");
                    }
                    return new IllegalArgumentException("Invoice not found");
                });

        // Patient/Doctor/Admin access check.
        validateBillingReadAccess(billing);

        try (
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            Document document = new Document();

            PdfWriter.getInstance(document, output);

            document.open();

            String patientName =
                    fullName(
                            billing.getPatient()
                                    .getAccount()
                                    .getFirstName(),

                            billing.getPatient()
                                    .getAccount()
                                    .getLastName()
                    );

            String doctorName =
                    fullName(
                            billing.getAppointment()
                                    .getDoctor()
                                    .getAccount()
                                    .getFirstName(),

                            billing.getAppointment()
                                    .getDoctor()
                                    .getAccount()
                                    .getLastName()
                    );

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd-MM-yyyy HH:mm"
                    );


            // -------------------------------------------------
            // PDF HEADER
            // -------------------------------------------------

            document.add(
                    new Paragraph("HEALTHNEXUS")
            );

            document.add(
                    new Paragraph("PAYMENT RECEIPT")
            );

            document.add(
                    new Paragraph(" ")
            );


            // -------------------------------------------------
            // BILLING TABLE
            // -------------------------------------------------

            Table table = new Table(2);

            table.setWidth(100);


            addRow(
                    table,
                    "Invoice Number",
                    billing.getInvoiceNumber()
            );

            addRow(
                    table,
                    "Patient",
                    patientName
            );

            addRow(
                    table,
                    "Doctor",
                    doctorName
            );

            addRow(
                    table,
                    "Appointment ID",
                    billing.getAppointment()
                            .getId()
                            .toString()
            );

            addRow(
                    table,
                    "Billing Date",
                    billing.getBillingDate()
                            .format(formatter)
            );

            addRow(
                    table,
                    "Amount",
                    "INR " + billing.getAmount()
            );

            addRow(
                    table,
                    "Payment Status",
                    billing.getPaymentStatus()
                            .name()
            );

            addRow(
                    table,
                    "Payment Method",
                    billing.getPaymentMethod() == null
                            ? "-"
                            : billing.getPaymentMethod().name()
            );

            addRow(
                    table,
                    "Payment Date",
                    billing.getPaymentDate() == null
                            ? "-"
                            : billing.getPaymentDate()
                                    .format(formatter)
            );


            document.add(table);

            document.add(
                    new Paragraph(" ")
            );

            document.add(
                    new Paragraph(
                            "Thank you for using HealthNexus."
                    )
            );

            document.close();

            return output.toByteArray();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to generate receipt PDF",
                    e
            );
        }
    }


    // =========================================================
    // INVOICE GENERATION SECURITY
    // =========================================================

    private void validateInvoiceGenerationAccess(
            AppointmentEntity appointment) {

        if (appointment == null || appointment.getHospital() == null) {
            throw new AccessDeniedException("Appointment or hospital details incomplete");
        }

        UUID resourceHospitalId = appointment.getHospital().getId();

        // ADMIN can generate invoices for their hospital.
        if (hasRole("ROLE_ADMIN")) {
            UUID currentHospitalId = tenantContextService.getCurrentUserHospitalId();
            if (currentHospitalId == null || !currentHospitalId.equals(resourceHospitalId)) {
                throw new AccessDeniedException("You can only generate invoices for your own hospital");
            }
            return;
        }

        // Anyone other than ADMIN/DOCTOR is denied.
        if (!hasRole("ROLE_DOCTOR")) {
            throw new AccessDeniedException(
                    "Only an admin or the assigned doctor can generate an invoice"
            );
        }

        UUID currentAccountId = SecurityUtils.getCurrentUser().getId();

        // Doctor must be assigned to this appointment.
        if (appointment.getDoctor() == null
                || appointment.getDoctor().getAccount() == null
                || !currentAccountId.equals(appointment.getDoctor().getAccount().getId())) {

            throw new AccessDeniedException(
                    "You can only generate invoices for your own appointments"
            );
        }

        // Also verify Doctor's hospital matches appointment's hospital.
        UUID doctorHospitalId = tenantContextService.getCurrentUserHospitalId();
        if (doctorHospitalId == null || !doctorHospitalId.equals(resourceHospitalId)) {
            throw new AccessDeniedException(
                    "You are not authorized to generate invoices for another hospital"
            );
        }
    }


    // =========================================================
    // BILLING READ SECURITY
    // =========================================================

    private void validateBillingReadAccess(
            BillingEntity billing) {

        if (billing == null ||
                billing.getAppointment() == null ||
                billing.getAppointment().getHospital() == null) {

            throw new AccessDeniedException(
                    "Billing information is incomplete"
            );
        }

        UUID resourceHospitalId =
                billing.getAppointment()
                        .getHospital()
                        .getId();

        /*
         * =========================================================
         * ADMIN
         * =========================================================
         *
         * Admin can access only the billing data belonging
         * to the Admin's assigned hospital.
         */
        if (hasRole("ROLE_ADMIN")) {

            UUID currentHospitalId =
                    tenantContextService
                            .getCurrentUserHospitalId();

            if (currentHospitalId == null ||
                    !currentHospitalId.equals(resourceHospitalId)) {

                throw new AccessDeniedException(
                        "You are not authorized to access billing from another hospital"
                );
            }

            return;
        }


        /*
         * =========================================================
         * RECEPTIONIST
         * =========================================================
         *
         * Receptionist can access billing only within
         * their assigned hospital.
         */
        if (hasRole("ROLE_RECEPTIONIST")) {

            UUID currentHospitalId =
                    tenantContextService
                            .getCurrentUserHospitalId();

            if (currentHospitalId == null ||
                    !currentHospitalId.equals(resourceHospitalId)) {

                throw new AccessDeniedException(
                        "You are not authorized to access billing from another hospital"
                );
            }

            return;
        }


        /*
         * =========================================================
         * DOCTOR
         * =========================================================
         *
         * Doctor can access only billing belonging to
         * their own appointment.
         */
        if (hasRole("ROLE_DOCTOR")) {

            UUID currentAccountId =
                    SecurityUtils
                            .getCurrentUser()
                            .getId();

            if (billing.getAppointment().getDoctor() == null ||
                    billing.getAppointment()
                            .getDoctor()
                            .getAccount() == null ||
                    !currentAccountId.equals(
                            billing.getAppointment()
                                    .getDoctor()
                                    .getAccount()
                                    .getId()
                    )) {

                throw new AccessDeniedException(
                        "You are not authorized to view this invoice"
                );
            }

            /*
             * Also verify Doctor's hospital.
             */
            UUID doctorHospitalId =
                    tenantContextService
                            .getCurrentUserHospitalId();

            if (doctorHospitalId == null ||
                    !doctorHospitalId.equals(resourceHospitalId)) {

                throw new AccessDeniedException(
                        "You are not authorized to access billing from another hospital"
                );
            }

            return;
        }


        /*
         * =========================================================
         * PATIENT
         * =========================================================
         */

        if (hasRole("ROLE_PATIENT")) {

            UUID currentAccountId =
                    SecurityUtils
                            .getCurrentUser()
                            .getId();

            if (billing.getPatient() == null ||
                    billing.getPatient().getAccount() == null ||
                    !currentAccountId.equals(
                            billing.getPatient()
                                    .getAccount()
                                    .getId()
                    )) {

                throw new AccessDeniedException(
                        "You are not authorized to view this invoice"
                );
            }

            return;
        }


        throw new AccessDeniedException(
                "You are not authorized to access this invoice"
        );
    }





    // =========================================================
    // ROLE CHECK
    // =========================================================

    private boolean hasRole(String role) {

        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(
                        authority ->
                                authority
                                        .getAuthority()
                                        .equals(role)
                );
    }


    // =========================================================
    // PDF TABLE ROW
    // =========================================================

    private void addRow(
            Table table,
            String key,
            String value) {

        table.addCell(
                new Cell(
                        new Paragraph(key)
                )
        );

        table.addCell(
                new Cell(
                        new Paragraph(
                                value == null
                                        ? "-"
                                        : value
                        )
                )
        );
    }


    // =========================================================
    // FULL NAME
    // =========================================================

    private String fullName(
            String first,
            String last) {

        return (
                (first == null ? "" : first)
                        + " "
                        +
                (last == null ? "" : last)
        ).trim();
    }


    // =========================================================
    // ENTITY → RESPONSE
    // =========================================================

    private BillingResponse mapToResponse(
            BillingEntity entity) {

        return BillingResponse.builder()

                .id(entity.getId())

                .appointmentId(
                        entity.getAppointment().getId()
                )

                .patientId(
                        entity.getPatient().getId()
                )

                .patientName(
                        fullName(
                                entity.getPatient()
                                        .getAccount()
                                        .getFirstName(),

                                entity.getPatient()
                                        .getAccount()
                                        .getLastName()
                        )
                )

                .doctorName(
                        fullName(
                                entity.getAppointment()
                                        .getDoctor()
                                        .getAccount()
                                        .getFirstName(),

                                entity.getAppointment()
                                        .getDoctor()
                                        .getAccount()
                                        .getLastName()
                        )
                )

                .amount(
                        entity.getAmount()
                )

                .paymentStatus(
                        entity.getPaymentStatus().name()
                )

                .paymentMethod(
                        entity.getPaymentMethod() != null
                                ? entity.getPaymentMethod().name()
                                : null
                )

                .invoiceNumber(
                        entity.getInvoiceNumber()
                )

                .billingDate(
                        entity.getBillingDate()
                )

                .paymentDate(
                        entity.getPaymentDate()
                )

                .build();
    }
}
