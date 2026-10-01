package com.tailoredplatform.ecommerce.users.service;

import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.users.dto.UpdateProfileRequest;
import com.tailoredplatform.ecommerce.users.entity.User;
import com.tailoredplatform.ecommerce.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;

    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        return userRepository.save(user);
    }
}
