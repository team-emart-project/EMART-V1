package com.emart.util;

import java.time.Year;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class OrderNumberGeneratorUtil {

    private final AtomicInteger sequence = new AtomicInteger(1);

    public String generate() {

        int current = sequence.getAndIncrement();

        return String.format("ORD-%d-%06d",
                Year.now().getValue(),
                current);
    }
}