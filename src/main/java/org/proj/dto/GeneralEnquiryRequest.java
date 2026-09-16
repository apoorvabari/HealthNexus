package org.proj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralEnquiryRequest {

    @NotBlank(message = "Inquirer name is required")
    @NotNull
    private String inquirerName;

    @NotBlank(message = "Contact number is required")
    @NotNull
    private String contactNumber;

    @NotBlank(message = "Enquiry text is required")
    @NotNull
    private String enquiryText;

    private String resolutionNotes;

    private String status;
}
