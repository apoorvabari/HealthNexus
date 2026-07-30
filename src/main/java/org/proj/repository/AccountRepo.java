package org.proj.repository;

import java.util.List;
import java.util.Optional;

import org.proj.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountRepo extends JpaRepository<AccountEntity, UUID> {

        Optional<AccountEntity> findByEmailIgnoreCase(String email);

        boolean existsByEmailIgnoreCase(String email);

        boolean existsByPhoneNumber(String phoneNumber);

        List<AccountEntity> findByIsActiveTrue();

        List<AccountEntity> findByIsDeletedFalse();

        @Query("SELECT a FROM AccountEntity a WHERE " +
                        "(:id IS NULL OR a.id = :id) AND " +
                        "(:firstName IS NULL OR LOWER(a.firstName) = LOWER(:firstName)) AND " +
                        "(:middleName IS NULL OR LOWER(a.middleName) = LOWER(:middleName)) AND " +
                        "(:lastName IS NULL OR LOWER(a.lastName) = LOWER(:lastName)) AND " +
                        "(:email IS NULL OR LOWER(a.email) = LOWER(:email)) AND " +
                        "(:phoneNumber IS NULL OR a.phoneNumber = :phoneNumber) AND " +
                        "(:isActive IS NULL OR a.isActive = :isActive) AND " +
                        "(:isDeleted IS NULL OR a.isDeleted = :isDeleted)")
        List<AccountEntity> filterAccounts(
                        @Param("id") UUID id,
                        @Param("firstName") String firstName,
                        @Param("middleName") String middleName,
                        @Param("lastName") String lastName,
                        @Param("email") String email,
                        @Param("phoneNumber") String phoneNumber,
                        @Param("isActive") Boolean isActive,
                        @Param("isDeleted") Boolean isDeleted);
}