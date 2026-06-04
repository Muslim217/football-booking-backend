package com.football.booking.service;

import com.football.booking.dto.request.UpdateProfileRequest;
import com.football.booking.dto.response.UserProfileResponse;
import com.football.booking.entity.User;
import com.football.booking.exception.ResourceNotFoundException;
import com.football.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getMyProfile(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return mapToResponse(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private UserProfileResponse mapToResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
