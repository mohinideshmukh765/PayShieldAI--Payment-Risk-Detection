package com.payshield.entity;

import com.payshield.entity.enums.TransactionStatus;
import com.payshield.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(
                        name = "idx_transactions_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_transactions_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_transactions_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "transaction_reference",
            nullable = false,
            unique = true,
            length = 50
    )
    private String transactionReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_user")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "source_account", nullable = false, length = 100)
    private String sourceAccount;

    @Column(name = "destination_account", nullable = false, length = 100)
    private String destinationAccount;

    @Column(name = "transaction_time", nullable = false)
    private Instant transactionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}