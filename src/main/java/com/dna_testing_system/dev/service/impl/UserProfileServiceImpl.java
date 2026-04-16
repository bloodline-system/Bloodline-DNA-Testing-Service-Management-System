package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.dto.response.UserResponse;
import com.dna_testing_system.dev.dto.response.profile.ProfileResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.ResourceNotFoundException;
import com.dna_testing_system.dev.mapper.UserProfileMapper;
import com.dna_testing_system.dev.repository.UserProfileRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    @Override
    @Transactional
    public boolean updateUserProfile(String username, UserProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
        UserProfile profile = user.getProfile();

        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profile);
        }

        // Ensure email is never null (DB requires it)
        String effectiveEmail = requestEmail;
        if (effectiveEmail == null || effectiveEmail.isBlank()) {
            if (currentEmail != null && !currentEmail.isBlank()) {
                effectiveEmail = currentEmail;
            } else if (signUpEmail != null && !signUpEmail.isBlank()) {
                effectiveEmail = signUpEmail;
            } else {
                throw new IllegalArgumentException("Email is required");
            }
        }
        request.setEmail(effectiveEmail.trim());

        // Validate email uniqueness if email is being updated
        String newEmail = request.getEmail();
        if (currentEmail == null || !currentEmail.equalsIgnoreCase(newEmail)) {
            boolean emailExists = userProfileRepository.findAll().stream()
                    .filter(up -> up.getEmail() != null)
                    .anyMatch(up -> up.getEmail().equalsIgnoreCase(newEmail) && !up.getUser().getId().equals(user.getId()));

            if (emailExists) {
                throw new IllegalArgumentException("Email already in use by another user");
            }
        }

        userProfileMapper.updateUserProfileFromDto(request, profile);

        // Avoid storing whitespace-only email
        if (request.getEmail() != null && request.getEmail().trim().isEmpty()) {
            profile.setEmail(null);
        }

        // Save user
        userRepository.save(user);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
        return userProfileMapper.toDto(user);
    }

    @Override
    @Transactional
    public boolean deleteUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
        if (user.getProfile() != null) {
            userProfileRepository.delete(user.getProfile());
            user.setProfile(null);
            userRepository.save(user);
        }
        // Nếu muốn xóa luôn user thì có thể gọi userRepository.delete(user);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUserProfileByName(String name) {
        if (name == null || name.isBlank()) {
            return getUserProfiles();
        }

        String keyword = name.trim().toLowerCase();

        return userRepository.findAll().stream()
                .filter(user -> user.getProfile() != null)
                .filter(user -> {
                    UserProfile profile = user.getProfile();
                    String firstName = profile.getFirstName();
                    String lastName = profile.getLastName();
                    String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();

                    return (firstName != null && firstName.toLowerCase().contains(keyword))
                            || (lastName != null && lastName.toLowerCase().contains(keyword))
                            || (!fullName.isEmpty() && fullName.toLowerCase().contains(keyword));
                })
                .map(userProfileMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUserProfiles() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(userProfileMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProfileResponse getProfile(String username)  {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
        return userProfileMapper.toProfileResponse(user);
    }
}