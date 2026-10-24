package org.proj.dto;

import jakarta.validation.constraints.*;
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
    @Size(min = 2, max = 100, message = "Inquirer name must be between 2 and 100 characters")
    private String inquirerName;

    @NotBlank(message = "Contact number is required")
    @jakarta.validation.constraints.Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String contactNumber;

    @NotBlank(message = "Enquiry text is required")
    @Size(min = 5, max = 1000, message = "Enquiry text must be between 5 and 1000 characters")
    private String enquiryText;

    @Size(max = 1000, message = "Resolution notes cannot exceed 1000 characters")
    private String resolutionNotes;

    private String status;
}
