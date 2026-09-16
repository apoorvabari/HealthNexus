package org.proj.repository;

import org.proj.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailIgnoreCase(
            String email
    );

    boolean existsByEmail(
            String email
    );

    boolean existsByEmailAndIdNot(
            String email,
            UUID id
    );

    boolean existsByPhoneNumber(
            String phoneNumber
    );

    boolean existsByPhoneNumberAndIdNot(
            String phoneNumber,
            UUID id
    );

    // =========================================================
    // GLOBAL SEARCH
    // =========================================================

    @Query("""
        SELECT DISTINCT u
        FROM UserEntity u
        WHERE u.isDeleted = false
        AND UPPER(u.role.roleName) <> 'ADMIN'
        AND (
            :search IS NULL
            OR :search = ''
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.middleName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    Page<UserEntity> searchUsers(
            @Param("search") String search,
            Pageable pageable
    );

    // =========================================================
    // HOSPITAL-SCOPED USER SEARCH
    // ADMIN USERS EXCLUDED
    // =========================================================

    @Query("""
        SELECT DISTINCT u
        FROM UserEntity u
        WHERE u.isDeleted = false
        AND UPPER(u.role.roleName) <> 'ADMIN'
        AND (
            EXISTS (
                SELECT 1
                FROM AdminEntity a
                WHERE a.account.id = u.id
                AND a.hospital.id = :hospitalId
            )
            OR EXISTS (
                SELECT 1
                FROM DoctorEntity d
                WHERE d.account.id = u.id
                AND d.hospital.id = :hospitalId
            )
            OR EXISTS (
                SELECT 1
                FROM ReceptionistEntity r
                WHERE r.account.id = u.id
                AND r.hospital.id = :hospitalId
            )
            OR EXISTS (
                SELECT 1
                FROM PatientEntity p
                WHERE p.account.id = u.id
                AND p.hospital.id = :hospitalId
            )
        )
        AND (
            :search IS NULL
            OR :search = ''
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.middleName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    Page<UserEntity> searchUsersByHospital(
            @Param("hospitalId") UUID hospitalId,
            @Param("search") String search,
            Pageable pageable
    );

    // =========================================================
    // HOSPITAL-SCOPED FILTER
    // ADMIN USERS EXCLUDED
    // =========================================================

    @Query("""
        SELECT DISTINCT u
        FROM UserEntity u
        WHERE u.isDeleted = false
        AND UPPER(u.role.roleName) <> 'ADMIN'
        AND (
            EXISTS (
                SELECT 1
                FROM AdminEntity a
                WHERE a.account.id = u.id
                AND a.hospital.id = :hospitalId
            )
            OR EXISTS (
                SELECT 1
                FROM DoctorEntity d
                WHERE d.account.id = u.id
                AND d.hospital.id = :hospitalId
            )
            OR EXISTS (
                SELECT 1
                FROM ReceptionistEntity r
                WHERE r.account.id = u.id
                AND r.hospital.id = :hospitalId
            )
            OR EXISTS (
                SELECT 1
                FROM PatientEntity p
                WHERE p.account.id = u.id
                AND p.hospital.id = :hospitalId
            )
        )
        AND (
            :id IS NULL
            OR u.id = :id
        )
        AND (
            :firstName IS NULL
            OR :firstName = ''
            OR LOWER(u.firstName)
                LIKE LOWER(CONCAT('%', :firstName, '%'))
        )
        AND (
            :middleName IS NULL
            OR :middleName = ''
            OR LOWER(u.middleName)
                LIKE LOWER(CONCAT('%', :middleName, '%'))
        )
        AND (
            :lastName IS NULL
            OR :lastName = ''
            OR LOWER(u.lastName)
                LIKE LOWER(CONCAT('%', :lastName, '%'))
        )
        AND (
            :email IS NULL
            OR :email = ''
            OR LOWER(u.email)
                LIKE LOWER(CONCAT('%', :email, '%'))
        )
        AND (
            :phoneNumber IS NULL
            OR :phoneNumber = ''
            OR u.phoneNumber
                LIKE CONCAT('%', :phoneNumber, '%')
        )
        AND (
            :status IS NULL
            OR :status = ''
            OR (
                :status = 'ACTIVE'
                AND u.isActive = true
            )
            OR (
                :status = 'INACTIVE'
                AND u.isActive = false
            )
        )
        """)
    List<UserEntity> filterUsersByHospital(
            @Param("hospitalId") UUID hospitalId,
            @Param("id") UUID id,
            @Param("firstName") String firstName,
            @Param("middleName") String middleName,
            @Param("lastName") String lastName,
            @Param("email") String email,
            @Param("phoneNumber") String phoneNumber,
            @Param("status") String status
    );

    @Query("""
        SELECT u FROM UserEntity u
        WHERE (
            :id IS NULL
            OR u.id = :id
        )
        AND (
            :firstName IS NULL
            OR :firstName = ''
            OR LOWER(u.firstName)
                LIKE LOWER(CONCAT('%', :firstName, '%'))
        )
        AND (
            :middleName IS NULL
            OR :middleName = ''
            OR LOWER(u.middleName)
                LIKE LOWER(CONCAT('%', :middleName, '%'))
        )
        AND (
            :lastName IS NULL
            OR :lastName = ''
            OR LOWER(u.lastName)
                LIKE LOWER(CONCAT('%', :lastName, '%'))
        )
        AND (
            :email IS NULL
            OR :email = ''
            OR LOWER(u.email)
                LIKE LOWER(CONCAT('%', :email, '%'))
        )
        AND (
            :phoneNumber IS NULL
            OR :phoneNumber = ''
            OR u.phoneNumber
                LIKE CONCAT('%', :phoneNumber, '%')
        )
        AND (
            :isActive IS NULL
            OR u.isActive = :isActive
        )
        AND (
            :isDeleted IS NULL
            OR u.isDeleted = :isDeleted
        )
        """)
    List<UserEntity> filterUsers(
            @Param("id") UUID id,
            @Param("firstName") String firstName,
            @Param("middleName") String middleName,
            @Param("lastName") String lastName,
            @Param("email") String email,
            @Param("phoneNumber") String phoneNumber,
            @Param("isActive") Boolean isActive,
            @Param("isDeleted") Boolean isDeleted
    );

    Optional<UserEntity> findByEmail(String email);

    // =========================================================
    // COUNTS
    // =========================================================

    long countByIsActiveTrue();

    long countByIsDeletedFalse();

    long countByIsActiveTrueAndIsDeletedFalse();
}