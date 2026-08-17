package com.payshield.entity.enums;

/**
 * WalletStatus — the operational state of a wallet.
 *
 * ACTIVE  : wallet can send and receive funds.
 * BLOCKED : wallet is frozen; debits and credits are rejected.
 *
 * NOTE: CLOSED has been removed — no code path sets or checks that value.
 * Re-add when a wallet closure flow is built.
 */
public enum WalletStatus {
    ACTIVE,
    BLOCKED
}