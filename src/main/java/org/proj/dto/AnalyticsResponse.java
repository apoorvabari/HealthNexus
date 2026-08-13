package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    private long totalDoctors;
    private long totalPatients;
    private long totalHospitals;
    private long totalAppointments;
    private long appointmentsToday;
}
