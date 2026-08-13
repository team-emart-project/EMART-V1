package com.example.demo.enums;

/**
 * Mirrors cart.status in the database.
 * ACTIVE    = the cart the user is currently filling
 * CONVERTED = the cart has been turned into an order (set by Module 8)
 */
public enum CartStatus {
    ACTIVE,
    CONVERTED
}
