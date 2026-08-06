package org.proj.controller;

import jakarta.validation.Valid;
import org.proj.dto.DepartmentRequest;
import org.proj.dto.DepartmentResponse;
import org.proj.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "http://localhost:8082")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse response = departmentService.createDepartment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<DepartmentResponse> getDepartmentById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDepartment(
            @PathVariable UUID id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok("Department deactivated successfully");
    }
}
