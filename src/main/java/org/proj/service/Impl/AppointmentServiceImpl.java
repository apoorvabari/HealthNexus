package org.proj.service.Impl;

import org.proj.dto.AppointmentRequest;
import org.proj.dto.AppointmentResponse;
import org.proj.entity.AppointmentEntity;
import org.proj.entity.HospitalEntity;
import org.proj.entity.DepartmentEntity;
import org.proj.entity.DoctorEntity;
import org.proj.entity.PatientEntity;
import org.proj.entity.ReceptionistEntity;
import org.proj.mapper.AppointmentMapper;
import org.proj.repository.AppointmentRepo;
import org.proj.repository.HospitalRepo;
import org.proj.repository.DepartmentRepo;
import org.proj.repository.DoctorRepo;
import org.proj.repository.PatientRepo;
import org.proj.repository.ReceptionistRepo;
import org.proj.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentRepo appointmentRepo;

    @Autowired
    private HospitalRepo hospitalRepo;

    @Autowired
    private DepartmentRepo departmentRepo;

    @Autowired
    private DoctorRepo doctorRepo;

    @Autowired
    private PatientRepo patientRepo;

    @Autowired
    private ReceptionistRepo receptionistRepo;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Override
    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        try {
            HospitalEntity hospital = hospitalRepo.findById(request.getHospitalId())
                    .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

            DepartmentEntity department = departmentRepo.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));

            DoctorEntity doctor = doctorRepo.findById(request.getDoctorId())
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

            PatientEntity patient = patientRepo.findById(request.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

            ReceptionistEntity receptionist = null;
            if (request.getBookedByReceptionistId() != null) {
                receptionist = receptionistRepo.findById(request.getBookedByReceptionistId())
                        .orElseThrow(() -> new IllegalArgumentException("Receptionist not found"));
            }

            if (doctor.getDepartment() == null || !doctor.getDepartment().getId().equals(request.getDepartmentId())) {
                throw new IllegalArgumentException("Doctor does not belong to the selected department");
            }

            if (doctor.getHospital() == null || !doctor.getHospital().getId().equals(request.getHospitalId())) {
                throw new IllegalArgumentException("Doctor does not belong to the selected hospital");
            }

            if (patient.getHospital() == null || !patient.getHospital().getId().equals(request.getHospitalId())) {
                throw new IllegalArgumentException("Patient does not belong to the selected hospital");
            }

            if (appointmentRepo.existsByDoctorIdAndAppointmentDateAndAppointmentTime(request.getDoctorId(), request.getAppointmentDate(), request.getAppointmentTime())) {
                throw new IllegalArgumentException("Doctor is not available at the selected date and time");
            }

            AppointmentEntity appointment = appointmentMapper.toEntity(request, hospital, department, doctor, patient, receptionist);
            
            String appointmentNumber;
            do {
                appointmentNumber = generateAppointmentCode(request.getAppointmentDate());
            } while (appointmentRepo.existsByAppointmentNumber(appointmentNumber));
            appointment.setAppointmentNumber(appointmentNumber);

            AppointmentEntity savedAppointment = appointmentRepo.save(appointment);
            AppointmentResponse response = appointmentMapper.toResponse(savedAppointment);
            response.setMessage("Appointment booked successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to book appointment.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Appointment Id is required");
            }
            AppointmentEntity appointment = appointmentRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
            return appointmentMapper.toResponse(appointment);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch appointment.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {
        try {
            return appointmentRepo.findAll().stream()
                    .map(appointmentMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch appointment list.", e);
        }
    }

    @Override
    @Transactional
    public AppointmentResponse updateAppointment(UUID id, AppointmentRequest request) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Appointment Id is required");
            }
            AppointmentEntity appointment = appointmentRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            HospitalEntity hospital = appointment.getHospital();
            if (request.getHospitalId() != null && !request.getHospitalId().equals(hospital.getId())) {
                hospital = hospitalRepo.findById(request.getHospitalId())
                        .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
            }

            DepartmentEntity department = appointment.getDepartment();
            if (request.getDepartmentId() != null && !request.getDepartmentId().equals(department.getId())) {
                department = departmentRepo.findById(request.getDepartmentId())
                        .orElseThrow(() -> new IllegalArgumentException("Department not found"));
            }

            DoctorEntity doctor = appointment.getDoctor();
            if (request.getDoctorId() != null && !request.getDoctorId().equals(doctor.getId())) {
                doctor = doctorRepo.findById(request.getDoctorId())
                        .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
            }

            PatientEntity patient = appointment.getPatient();
            if (request.getPatientId() != null && !request.getPatientId().equals(patient.getId())) {
                patient = patientRepo.findById(request.getPatientId())
                        .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
            }

            ReceptionistEntity receptionist = appointment.getBookedByReceptionist();
            if (request.getBookedByReceptionistId() != null && (receptionist == null || !request.getBookedByReceptionistId().equals(receptionist.getId()))) {
                receptionist = receptionistRepo.findById(request.getBookedByReceptionistId())
                        .orElseThrow(() -> new IllegalArgumentException("Receptionist not found"));
            }

            UUID targetDeptId = department.getId();
            UUID targetHospitalId = hospital.getId();
            UUID targetPatientHospitalId = (patient.getHospital() != null) ? patient.getHospital().getId() : null;

            if (doctor.getDepartment() == null || !doctor.getDepartment().getId().equals(targetDeptId)) {
                throw new IllegalArgumentException("Doctor does not belong to the selected department");
            }

            if (doctor.getHospital() == null || !doctor.getHospital().getId().equals(targetHospitalId)) {
                throw new IllegalArgumentException("Doctor does not belong to the selected hospital");
            }

            if (targetPatientHospitalId == null || !targetPatientHospitalId.equals(targetHospitalId)) {
                throw new IllegalArgumentException("Patient does not belong to the selected hospital");
            }

            LocalDate targetDate = request.getAppointmentDate() != null ? request.getAppointmentDate() : appointment.getAppointmentDate();
            LocalTime targetTime = request.getAppointmentTime() != null ? request.getAppointmentTime() : appointment.getAppointmentTime();
            UUID targetDoctorId = doctor.getId();

            if (appointmentRepo.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndIdNot(targetDoctorId, targetDate, targetTime, id)) {
                throw new IllegalArgumentException("Doctor is not available at the selected date and time");
            }

            appointmentMapper.updateEntity(appointment, request, hospital, department, doctor, patient, receptionist);
            AppointmentEntity updatedAppointment = appointmentRepo.save(appointment);
            
            AppointmentResponse response = appointmentMapper.toResponse(updatedAppointment);
            response.setMessage("Appointment updated successfully");
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to update appointment.", e);
        }
    }

    @Override
    @Transactional
    public void deleteAppointment(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Appointment Id is required");
            }
            AppointmentEntity appointment = appointmentRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            appointment.setAppointmentStatus(AppointmentEntity.AppointmentStatus.CANCELLED);
            appointmentRepo.save(appointment);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to cancel appointment.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getTodayAppointments() {
        try {
            return appointmentRepo.findByAppointmentDate(LocalDate.now()).stream()
                    .map(appointmentMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch today's appointments.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDoctor(UUID doctorId) {
        try {
            if (doctorId == null) {
                throw new IllegalArgumentException("Doctor Id is required");
            }
            return appointmentRepo.findByDoctorId(doctorId).stream()
                    .map(appointmentMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch doctor's appointments.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatient(UUID patientId) {
        try {
            if (patientId == null) {
                throw new IllegalArgumentException("Patient Id is required");
            }
            return appointmentRepo.findByPatientId(patientId).stream()
                    .map(appointmentMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch patient's appointments.", e);
        }
    }

    private String generateAppointmentCode(LocalDate date) {
        String dateStr = date.toString().replace("-", "");
        String rand = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return "APT-" + dateStr + "-" + rand;
    }
}
