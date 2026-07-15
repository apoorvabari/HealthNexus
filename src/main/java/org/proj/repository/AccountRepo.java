package org.proj.repository;

import java.util.Optional;

import org.proj.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepo extends JpaRepository<Account,Long>{
	Optional<Account> findByEmail(String email);
	
	boolean existsByEmail(String email);
	
}
