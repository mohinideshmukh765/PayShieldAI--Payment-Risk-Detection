package com.payshield.entity.enums;

/**
 * WalletTransactionType — the direction of a wallet ledger entry.
 *
 * CREDIT : funds added to the wallet (top-up, refund credit).
 * DEBIT  : funds deducted from the wallet (payment execution).
 *
 * NOTE: REFUND and REVERSAL have been removed — no refund or reversal flow exists yet.
 * Re-add when those features are built.
 */
public enum WalletTransactionType {
    CREDIT,
    DEBIT
}