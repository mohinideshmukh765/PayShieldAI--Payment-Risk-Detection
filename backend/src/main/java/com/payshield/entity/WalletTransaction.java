package com.payshield.entity;

import com.payshield.entity.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(
                        name = "idx_wallet_transactions_wallet_id",
                        columnList = "wallet_id"
                ),
                @Index(
                        name = "idx_wallet_transactions_payment_id",
                        columnList = "payment_id"
                ),
                @Index(
                        name = "idx_wallet_transactions_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletTransactionType type;

    @Column(
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal amount;

    @Column(
            name = "balance_before",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal balanceBefore;

    @Column(
            name = "balance_after",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal balanceAfter;

    @Column(length = 100)
    private String reference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}