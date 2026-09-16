package org.proj.repository;

import org.proj.entity.GeneralEnquiryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneralEnquiryRepo extends JpaRepository<GeneralEnquiryEntity, UUID> {
    List<GeneralEnquiryEntity> findByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);
}
