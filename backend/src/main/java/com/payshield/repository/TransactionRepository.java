package com.payshield.repository;

import com.payshield.entity.Transaction;
import com.payshield.entity.enums.TransactionStatus;
import com.payshield.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
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




        @Query("""
        SELECT COUNT(t) FROM Transaction t
        WHERE t.user.id = :userId
        AND t.transactionTime >= :since
    """)
        long countByUserIdSince(UUID userId, Instant since);

        @Query("""
        SELECT AVG(t.amount) FROM Transaction t
        WHERE t.user.id = :userId
        AND t.status = com.payshield.entity.enums.TransactionStatus.COMPLETED
    """)
        BigDecimal findAverageAmountByUserId(UUID userId);

        @Query("""
        SELECT COUNT(t) FROM Transaction t
        WHERE t.user.id = :userId
        AND t.status = com.payshield.entity.enums.TransactionStatus.BLOCKED
        AND t.transactionTime >= :since
    """)
        long countRecentFailedAttempts(UUID userId, Instant since);
    }
