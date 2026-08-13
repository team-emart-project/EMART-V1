package com.example.demo.service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.interfaces.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.securityUtils = securityUtils;
    }


    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;

    @Override
    public UserResponse getCurrentUser() {
        return userMapper.toResponse(loadCurrentUser());
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(UpdateProfileRequest request) {

        User user = loadCurrentUser();

        // Explicit field-by-field assignment (an allow-list). We never copy the
        // request wholesale onto the entity — that is how mass-assignment bugs
        // let someone promote themselves or flip isCardholder.
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName() == null ? null : request.getLastName().trim());
        user.setPhone(request.getPhone());
        user.setDob(request.getDob());
        user.setGender(request.getGender());
        user.setEducation(request.getEducation());
        user.setOccupation(request.getOccupation());
        user.setAnnualIncome(request.getAnnualIncome());

        // null means "not supplied" -> leave the existing preference alone
        if (request.getMarketingConsent() != null) {
            user.setMarketingConsent(request.getMarketingConsent());
        }

        User saved = userRepository.save(user);
        log.debug("Updated profile for userId={}", saved.getUserId());

        return userMapper.toResponse(saved);
    }

    private User loadCurrentUser() {
        Integer userId = securityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
    }
}
