package com.payshield.repository;

import com.payshield.entity.Transaction;
import com.payshield.entity.enums.TransactionStatus;
import com.payshield.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID> {

    boolean existsByTransactionReference(String transactionReference);

    Page<Transaction> findByStatus(
            TransactionStatus status,
            Pageable pageable
    );

    Page<Transaction> findByTransactionType(
            TransactionType transactionType,
            Pageable pageable
    );

    Page<Transaction> findByStatusAndTransactionType(
            TransactionStatus status,
            TransactionType transactionType,
            Pageable pageable
    );
}