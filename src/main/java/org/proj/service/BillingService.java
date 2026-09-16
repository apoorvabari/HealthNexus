package org.proj.service;

import org.proj.dto.BillingRequest;
import org.proj.dto.BillingResponse;

import java.util.List;
import java.util.UUID;

public interface BillingService {

    BillingResponse generateInvoice(BillingRequest request);

    BillingResponse processPayment(UUID billingId, String paymentMethod);

    BillingResponse getBillingById(UUID id);

    BillingResponse getBillingByAppointmentId(UUID appointmentId);

    List<BillingResponse> getBillingByPatientId(UUID patientId);

    byte[] generateReceiptPdf(UUID billingId);
}
