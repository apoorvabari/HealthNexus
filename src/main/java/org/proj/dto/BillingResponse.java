package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingResponse {
    
    private UUID id;
    private UUID appointmentId;
    private UUID patientId;
    private String patientName;
    private String doctorName;
    private BigDecimal amount;
    private String paymentStatus;
    private String paymentMethod;
    private String invoiceNumber;
    private LocalDateTime billingDate;
    private LocalDateTime paymentDate;
}
