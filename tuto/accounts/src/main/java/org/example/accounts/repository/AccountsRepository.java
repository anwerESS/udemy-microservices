package org.example.accounts.repository;

import jakarta.transaction.Transactional;
import org.example.accounts.entity.Accounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountsRepository extends JpaRepository<org.example.accounts.entity.Accounts, Long> {

    Optional<Accounts > findByCustomerId(Long customerId);

    // Marks this method as transactional, meaning it will be executed within a database transaction.
    // If an exception occurs, the transaction will be rolled back.
    @Transactional

    // Indicates that this method will modify the database (e.g., DELETE, UPDATE, INSERT).
    // Required for non-query methods (like `deleteBy`) in Spring Data JPA.
    @Modifying

    void deleteByCustomerId(Long customerId);

}