package com.example.demo.util;

import com.example.demo.repository.EmartCardRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Generates e-MART card numbers, e.g. EMCARD-1048372. */
@Component
public class CardNumberGeneratorUtil {
    public CardNumberGeneratorUtil(EmartCardRepository emartCardRepository) {
        this.emartCardRepository = emartCardRepository;
    }


    private static final String PREFIX = "EMCARD-";
    private static final int MAX_ATTEMPTS = 10;

    private final EmartCardRepository emartCardRepository;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = PREFIX + (1_000_000 + random.nextInt(9_000_000));
            if (!emartCardRepository.existsByCardNumber(candidate)) {
                return candidate;
            }
        }
        return PREFIX + System.currentTimeMillis();
    }
}
