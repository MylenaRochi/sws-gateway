package com.senior.sws_gateway.repository;

import com.senior.sws_gateway.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    
    /**
     * Find user account by email address
     * @param email the email address
     * @return Optional containing the user account if found
     */
    Optional<UserAccount> findByEmail(String email);
    
    /**
     * Check if a user account exists with the given email
     * @param email the email address
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
}