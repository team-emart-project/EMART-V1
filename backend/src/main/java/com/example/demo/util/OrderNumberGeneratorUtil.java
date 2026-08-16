package com.example.demo.util;

import com.example.demo.repository.OrdersRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Year;

/** Generates customer-facing order numbers, e.g. ORD-2026-048372. */
@Component
public class OrderNumberGeneratorUtil {

    private static final int MAX_ATTEMPTS = 10;

    private final OrdersRepository ordersRepository;
    private final SecureRandom random = new SecureRandom();

    public OrderNumberGeneratorUtil(OrdersRepository ordersRepository) {
        this.ordersRepository = ordersRepository;
    }

    public String generate() {
        int year = Year.now().getValue();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = "ORD-%d-%06d".formatted(year, random.nextInt(1_000_000));
            if (!ordersRepository.existsByOrderNo(candidate)) {
                return candidate;
            }
        }
        return "ORD-%d-%d".formatted(year, System.currentTimeMillis());
    }
}
