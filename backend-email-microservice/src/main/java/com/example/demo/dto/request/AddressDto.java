package com.example.demo.dto.request;

/**
 * A delivery or billing address, flattened for printing.
 *
 * Nothing here is @NotBlank: the email must still go out if an address is
 * partially filled, and the template simply skips the empty pieces.
 */
public record AddressDto(

        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String zipCode,
        String country

) {
    /** "12 MG Road, Indore, Madhya Pradesh 452001, India" — blanks dropped. */
    public String singleLine() {
        StringBuilder sb = new StringBuilder();
        append(sb, addressLine1);
        append(sb, addressLine2);
        append(sb, city);

        String stateAndZip = joinNonBlank(state, zipCode);
        append(sb, stateAndZip);
        append(sb, country);
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) return;
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(part.trim());
    }

    private static String joinNonBlank(String a, String b) {
        boolean hasA = a != null && !a.isBlank();
        boolean hasB = b != null && !b.isBlank();
        if (hasA && hasB) return a.trim() + " " + b.trim();
        if (hasA) return a.trim();
        return hasB ? b.trim() : null;
    }
}
