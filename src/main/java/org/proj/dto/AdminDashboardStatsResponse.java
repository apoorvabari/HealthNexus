package org.proj.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {

    // Doctors
    private long totalDoctors;
    private long pendingDoctors;
    private long approvedDoctors;
    private long rejectedDoctors;

    // Hospitals / Clinics
    private long totalHospitals;
    private long pendingHospitals;
    private long approvedHospitals;
    private long rejectedHospitals;

    // Users
    private long totalUsers;
    private long activeUsers;
    private long deletedUsers;
}