package com.example.demo.util;

import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates the membership number emailed to a new member (e.g. EMART04837).
 *
 * Uses SecureRandom plus a uniqueness check rather than "count + 1", because
 * counting rows is racy: two people registering at the same instant would both
 * read the same count and generate the same number.
 */
@Component
public class MembershipNumberGeneratorUtil {
    public MembershipNumberGeneratorUtil(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    private static final String PREFIX = "EMART";
    private static final int DIGITS = 5;
    private static final int MAX_ATTEMPTS = 10;

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = PREFIX + String.format("%0" + DIGITS + "d", random.nextInt(100_000));
            if (!userRepository.existsByMembershipNo(candidate)) {
                return candidate;
            }
        }
        // Astronomically unlikely; fall back to a wider namespace rather than loop forever.
        return PREFIX + System.currentTimeMillis();
    }
}
