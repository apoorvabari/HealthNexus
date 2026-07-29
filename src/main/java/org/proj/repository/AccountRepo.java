package org.proj.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.proj.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepo extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findByEmail(String email);
    Optional<AccountEntity> findByEmailIgnoreCase(String email);
    Optional<AccountEntity> findByUserId(UUID userId);

    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    List<AccountEntity> findByIsActiveTrue();

    List<AccountEntity> findByIsDeletedFalse();

    @Query("SELECT a FROM AccountEntity a WHERE " +
            "(:id IS NULL OR a.userId = :id) AND " +
            "(:firstName IS NULL OR LOWER(a.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
            "(:middleName IS NULL OR LOWER(a.middleName) LIKE LOWER(CONCAT('%', :middleName, '%'))) AND " +
            "(:lastName IS NULL OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
            "(:email IS NULL OR LOWER(a.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:phoneNumber IS NULL OR a.phoneNumber LIKE CONCAT('%', :phoneNumber, '%')) AND " +
            "(:isActive IS NULL OR a.isActive = :isActive) AND " +
            "(:isDeleted IS NULL OR a.isDeleted = :isDeleted)")
    List<AccountEntity> filterAccounts(
            UUID id,
            String firstName,
            String middleName,
            String lastName,
            String email,
            String phoneNumber,
            Boolean isActive,
            Boolean isDeleted);
}