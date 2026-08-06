package org.proj.repository;

import java.util.List;
import java.util.Optional;

import org.proj.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, UUID> {

        Optional<UserEntity> findByEmail(String email);

        boolean existsByEmail(String email);

        boolean existsByPhoneNumber(String phoneNumber);

        List<UserEntity> findByIsActiveTrue();

        List<UserEntity> findByIsDeletedFalse();

        List<UserEntity> findByIsActiveTrueAndIsDeletedFalse();

        @Query("SELECT u FROM UserEntity u WHERE " +
                        "(:id IS NULL OR u.id = :id) AND " +
                        "(:firstName IS NULL OR LOWER(u.firstName) = LOWER(:firstName)) AND " +
                        "(:middleName IS NULL OR LOWER(u.middleName) = LOWER(:middleName)) AND " +
                        "(:lastName IS NULL OR LOWER(u.lastName) = LOWER(:lastName)) AND " +
                        "(:email IS NULL OR LOWER(u.email) = LOWER(:email)) AND " +
                        "(:phoneNumber IS NULL OR u.phoneNumber = :phoneNumber) AND " +
                        "(:isActive IS NULL OR u.isActive = :isActive) AND " +
                        "(:isDeleted IS NULL OR u.isDeleted = :isDeleted)")
        List<UserEntity> filterUsers(
                        @Param("id") UUID id,
                        @Param("firstName") String firstName,
                        @Param("middleName") String middleName,
                        @Param("lastName") String lastName,
                        @Param("email") String email,
                        @Param("phoneNumber") String phoneNumber,
                        @Param("isActive") Boolean isActive,
                        @Param("isDeleted") Boolean isDeleted);
}
