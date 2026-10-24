package org.proj.exception;

import lombok.Getter;
import org.proj.dto.DoctorLocationConflictResponse;

@Getter
public class DoctorLocationConflictException extends RuntimeException {

    private final DoctorLocationConflictResponse conflictData;

    public DoctorLocationConflictException(DoctorLocationConflictResponse conflictData) {
        super(conflictData != null ? conflictData.getMessage() : "Doctor is currently on active duty at another location");
        this.conflictData = conflictData;
    }
}
