package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentAnalyticsResponse {

    private long totalDoctors;
    private long activeDoctors;
    private long inactiveDoctors;
    private long suspendedDoctors;
}