package org.proj.mapper;

import org.proj.dto.AppointmentResponse;
import org.proj.dto.AppointmentRequest;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.PatientEntity;
import org.proj.entity.ReceptionistEntity;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentEntity toEntity(AppointmentRequest request, HospitalEntity hospital, DepartmentEntity department, DoctorEntity doctor, PatientEntity patient, ReceptionistEntity receptionist) {
        if (request == null) {
            return null;
        }
        return AppointmentEntity.builder()
                .hospital(hospital)
                .department(department)
                .doctor(doctor)
                .patient(patient)
                .bookedByReceptionist(receptionist)
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .appointmentType(request.getAppointmentType())
                .appointmentStatus(request.getAppointmentStatus() != null ? request.getAppointmentStatus() : AppointmentEntity.AppointmentStatus.SCHEDULED)
                .consultationMode(request.getConsultationMode() != null ? request.getConsultationMode() : AppointmentEntity.ConsultationMode.OPD)
                .remarks(request.getRemarks())
                .build();
    }

    public void updateEntity(AppointmentEntity appointment, AppointmentRequest request, HospitalEntity hospital, DepartmentEntity department, DoctorEntity doctor, PatientEntity patient, ReceptionistEntity receptionist) {
        if (appointment == null || request == null) {
            return;
        }
        if (hospital != null) {
            appointment.setHospital(hospital);
        }
        if (department != null) {
            appointment.setDepartment(department);
        }
        if (doctor != null) {
            appointment.setDoctor(doctor);
        }
        if (patient != null) {
            appointment.setPatient(patient);
        }
        if (receptionist != null) {
            appointment.setBookedByReceptionist(receptionist);
        }
        appointment.setAppointmentDate(request.getAppointmentDate() != null ? request.getAppointmentDate() : appointment.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime() != null ? request.getAppointmentTime() : appointment.getAppointmentTime());
        appointment.setAppointmentType(request.getAppointmentType() != null ? request.getAppointmentType() : appointment.getAppointmentType());
        appointment.setAppointmentStatus(request.getAppointmentStatus() != null ? request.getAppointmentStatus() : appointment.getAppointmentStatus());
        appointment.setConsultationMode(request.getConsultationMode() != null ? request.getConsultationMode() : appointment.getConsultationMode());
        appointment.setRemarks(request.getRemarks() != null ? request.getRemarks() : appointment.getRemarks());
    }

    public AppointmentResponse toResponse(AppointmentEntity appointment) {
        if (appointment == null) {
            return null;
        }
        String doctorName = "";
        if (appointment.getDoctor() != null && appointment.getDoctor().getAccount() != null) {
            String first = appointment.getDoctor().getAccount().getFirstName() != null ? appointment.getDoctor().getAccount().getFirstName() : "";
            String last = appointment.getDoctor().getAccount().getLastName() != null ? appointment.getDoctor().getAccount().getLastName() : "";
            doctorName = (first + " " + last).trim();
        }
        String patientName = "";
        if (appointment.getPatient() != null && appointment.getPatient().getAccount() != null) {
            String first = appointment.getPatient().getAccount().getFirstName() != null ? appointment.getPatient().getAccount().getFirstName() : "";
            String last = appointment.getPatient().getAccount().getLastName() != null ? appointment.getPatient().getAccount().getLastName() : "";
            patientName = (first + " " + last).trim();
        }
        String receptionistName = "";
        if (appointment.getBookedByReceptionist() != null && appointment.getBookedByReceptionist().getAccount() != null) {
            String first = appointment.getBookedByReceptionist().getAccount().getFirstName() != null ? appointment.getBookedByReceptionist().getAccount().getFirstName() : "";
            String last = appointment.getBookedByReceptionist().getAccount().getLastName() != null ? appointment.getBookedByReceptionist().getAccount().getLastName() : "";
            receptionistName = (first + " " + last).trim();
        }
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .appointmentNumber(appointment.getAppointmentNumber())
                .hospitalId(appointment.getHospital() != null ? appointment.getHospital().getId() : null)
                .hospitalName(appointment.getHospital() != null ? appointment.getHospital().getHospitalName() : null)
                .departmentId(appointment.getDepartment() != null ? appointment.getDepartment().getId() : null)
                .departmentName(appointment.getDepartment() != null ? appointment.getDepartment().getDepartmentName() : null)
                .doctorId(appointment.getDoctor() != null ? appointment.getDoctor().getId() : null)
                .doctorName(doctorName)
                .patientId(appointment.getPatient() != null ? appointment.getPatient().getId() : null)
                .patientName(patientName)
                .bookedByReceptionistId(appointment.getBookedByReceptionist() != null ? appointment.getBookedByReceptionist().getId() : null)
                .bookedByReceptionistName(receptionistName)
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentType(appointment.getAppointmentType())
                .appointmentStatus(appointment.getAppointmentStatus())
                .consultationMode(appointment.getConsultationMode())
                .remarks(appointment.getRemarks())
                .createdAt(appointment.getCreatedAt())
                .doctorSpecialization(appointment.getDoctor() != null ? appointment.getDoctor().getSpecialization() : null)
                .consultationFee(appointment.getDoctor() != null ? appointment.getDoctor().getConsultationFee() : null)
                .queueNumber(appointment.getQueue() != null ? appointment.getQueue().getQueueNumber() : null)
                .tokenNumber(appointment.getQueue() != null ? appointment.getQueue().getTokenNumber() : null)
                .queueStatus(appointment.getQueue() != null && appointment.getQueue().getQueueStatus() != null ? appointment.getQueue().getQueueStatus().name() : null)
                .message("Success")
                .build();
    }
}
