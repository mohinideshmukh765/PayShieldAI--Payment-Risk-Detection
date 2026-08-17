package com.payshield.entity.enums;

/**
 * TransactionType — the category of a transaction record.
 *
 * PAYMENT : standard outgoing payment initiated by the user through the payment gateway.
 *
 * NOTE: TRANSFER, CASH_OUT, CASH_IN, DEBIT, CREDIT have been removed — they were never
 * assigned by any service or controller. Re-add specific types when those flows are built.
 */
public enum TransactionType {
    PAYMENT
}
