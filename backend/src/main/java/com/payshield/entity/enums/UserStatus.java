package com.payshield.entity.enums;

/**
 * UserStatus — the lifecycle state of a user account.
 *
 * ACTIVE    : account is in good standing and can authenticate.
 * SUSPENDED : account has been temporarily disabled by an admin.
 *
 * NOTE: INACTIVE and LOCKED have been removed — no code path sets or checks
 * those values. Re-add when account lockout / deactivation flows are built.
 */
public enum UserStatus {
    ACTIVE,
    SUSPENDED
}