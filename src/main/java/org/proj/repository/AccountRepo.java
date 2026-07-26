package org.proj.repository;

import java.util.List;
import java.util.Optional;

import org.proj.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepo extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findByEmail(String email);
    Optional<AccountEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    List<AccountEntity> findByIsActiveTrue();

    List<AccountEntity> findByIsDeletedFalse();

    @Query("SELECT a FROM AccountEntity a WHERE " +
            "(:id IS NULL OR a.id = :id) AND " +
            "(:name IS NULL OR " +
            "  LOWER(a.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "  LOWER(a.middleName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "  LOWER(a.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:email IS NULL OR LOWER(a.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:phoneNumber IS NULL OR a.phoneNumber LIKE CONCAT('%', :phoneNumber, '%')) AND " +
            "(:isActive IS NULL OR a.isActive = :isActive) AND " +
            "(:isDeleted IS NULL OR a.isDeleted = :isDeleted)")
    List<AccountEntity> filterAccounts(
            Long id,
            String name,
            String email,
            String phoneNumber,
            Boolean isActive,
            Boolean isDeleted);
}