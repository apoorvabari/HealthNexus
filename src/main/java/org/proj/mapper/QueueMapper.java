package org.proj.mapper;

import org.proj.dto.QueueResponse;
import org.proj.entity.QueueEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.AppointmentEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class QueueMapper {

    public QueueEntity toEntity(Integer queueNumber, String tokenNumber, HospitalEntity hospital, DepartmentEntity department, DoctorEntity doctor, AppointmentEntity appointment) {
        return QueueEntity.builder()
                .queueNumber(queueNumber)
                .tokenNumber(tokenNumber)
                .hospital(hospital)
                .department(department)
                .doctor(doctor)
                .appointment(appointment)
                .queueStatus(QueueEntity.QueueStatus.WAITING)
                .checkedInTime(LocalDateTime.now())
                .build();
    }

    public QueueResponse toResponse(QueueEntity queue) {
        if (queue == null) {
            return null;
        }
        String doctorName = "";
        if (queue.getDoctor() != null && queue.getDoctor().getAccount() != null) {
            String first = queue.getDoctor().getAccount().getFirstName() != null ? queue.getDoctor().getAccount().getFirstName() : "";
            String last = queue.getDoctor().getAccount().getLastName() != null ? queue.getDoctor().getAccount().getLastName() : "";
            doctorName = (first + " " + last).trim();
        }
        
        String patientName = "";
        if (queue.getAppointment() != null && queue.getAppointment().getPatient() != null && queue.getAppointment().getPatient().getAccount() != null) {
            String first = queue.getAppointment().getPatient().getAccount().getFirstName() != null ? queue.getAppointment().getPatient().getAccount().getFirstName() : "";
            String last = queue.getAppointment().getPatient().getAccount().getLastName() != null ? queue.getAppointment().getPatient().getAccount().getLastName() : "";
            patientName = (first + " " + last).trim();
        }
        
        return QueueResponse.builder()
                .id(queue.getId())
                .queueNumber(queue.getQueueNumber())
                .tokenNumber(queue.getTokenNumber())
                .hospitalId(queue.getHospital() != null ? queue.getHospital().getId() : null)
                .hospitalName(queue.getHospital() != null ? queue.getHospital().getHospitalName() : null)
                .departmentId(queue.getDepartment() != null ? queue.getDepartment().getId() : null)
                .departmentName(queue.getDepartment() != null ? queue.getDepartment().getDepartmentName() : null)
                .doctorId(queue.getDoctor() != null ? queue.getDoctor().getId() : null)
                .doctorName(doctorName)
                .patientId(queue.getAppointment() != null && queue.getAppointment().getPatient() != null ? queue.getAppointment().getPatient().getId() : null)
                .patientName(patientName)
                .appointmentId(queue.getAppointment() != null ? queue.getAppointment().getId() : null)
                .appointmentNumber(queue.getAppointment() != null ? queue.getAppointment().getAppointmentNumber() : null)
                .queueStatus(queue.getQueueStatus())
                .checkedInTime(queue.getCheckedInTime())
                .consultationStart(queue.getConsultationStart())
                .consultationEnd(queue.getConsultationEnd())
                .message("Success")
                .build();
    }
}
