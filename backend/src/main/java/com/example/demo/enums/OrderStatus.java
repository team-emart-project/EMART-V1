package com.example.demo.enums;

/**
 * Mirrors orders.order_status.
 *
 * Only PLACED -> PAID -> CANCELLED are reachable in this phase. SHIPPED and
 * DELIVERED exist in the schema but nothing can set them, because moving an
 * order along after payment is an admin action and admin is out of scope.
 */
public enum OrderStatus {
    PLACED,
    PAID,
    CANCELLED,
    SHIPPED,
    DELIVERED
}
