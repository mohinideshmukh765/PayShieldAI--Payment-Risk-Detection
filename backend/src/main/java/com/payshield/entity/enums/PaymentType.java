package com.payshield.entity.enums;

/**
 * PaymentType — describes the category of a payment record.
 *
 * PAYMENT  : standard outgoing payment initiated by the user.
 *
 * NOTE: REFUND has been intentionally removed — no refund flow exists yet
 * in any service or controller. Re-add when the refund feature is built.
 */
public enum PaymentType {
    PAYMENT
}