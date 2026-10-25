package com.ramya.mealmate.model;

/** Lifecycle: PENDING_PAYMENT -> PAID -> PREPARING -> OUT_FOR_DELIVERY -> DELIVERED, or CANCELLED. */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
