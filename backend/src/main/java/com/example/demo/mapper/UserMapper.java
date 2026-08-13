package com.example.demo.mapper;

import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .membershipNo(user.getMembershipNo())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dob(user.getDob())
                .gender(user.getGender())
                .education(user.getEducation())
                .occupation(user.getOccupation())
                .annualIncome(user.getAnnualIncome())
                .marketingConsent(user.getMarketingConsent())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .cardholder(user.getIsCardholder())
                .active(user.getIsActive())
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : null)
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .build();
        // NOTE: passwordHash, googleSub and resetPasswordToken are intentionally
        // omitted. googleSub is Google's internal id for the account — the UI
        // has no use for it, and anything not sent cannot leak.
    }
}
