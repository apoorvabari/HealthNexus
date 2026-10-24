package org.proj.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAffiliationRequest {

    @NotNull(message = "Approval status is required")
    private Boolean approved;

    @Size(max = 500, message = "Admin notes cannot exceed 500 characters")
    private String adminNotes;
}
