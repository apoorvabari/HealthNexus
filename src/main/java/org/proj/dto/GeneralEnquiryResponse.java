package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralEnquiryResponse {

    private UUID id;
    private UUID hospitalId;
    private String hospitalName;
    private String inquirerName;
    private String contactNumber;
    private String enquiryText;
    private String resolutionNotes;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
}
