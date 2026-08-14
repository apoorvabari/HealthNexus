package org.proj.repository;

import java.util.List;
import java.util.Optional;

import org.proj.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        long countByIsActiveTrue();

        long countByIsDeletedFalse();

        long countByIsActiveTrueAndIsDeletedFalse();
                
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
                        UUID id,
                        String firstName,
                        String middleName,
                        String lastName,
                        String email,
                        String phoneNumber,
                        Boolean isActive,
                        Boolean isDeleted);

        @Query("SELECT u FROM UserEntity u WHERE " +
               "(:search IS NULL OR :search = '' OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
               "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
               "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<UserEntity> searchUsers(String search, Pageable pageable);
}
