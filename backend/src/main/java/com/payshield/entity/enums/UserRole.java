package com.payshield.entity.enums;

/**
 * UserRole — the access tier of a registered account.
 *
 * USER    : consumer account; can make payments, top up wallet, and view payment history.
 * ANALYST : risk officer; can inspect transactions, evaluate fraud rules, and approve/reject flagged payments.
 */
public enum UserRole {
    USER,
    ANALYST
}