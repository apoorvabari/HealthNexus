package org.proj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingsRequest {
    @NotBlank(message = "Setting key is required")
    @jakarta.validation.constraints.Size(min = 2, max = 100, message = "Setting key must be between 2 and 100 characters")
    private String settingKey;

    @NotBlank(message = "Setting value is required")
    @jakarta.validation.constraints.Size(min = 1, max = 1000, message = "Setting value must be between 1 and 1000 characters")
    private String settingValue;

    @jakarta.validation.constraints.Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
