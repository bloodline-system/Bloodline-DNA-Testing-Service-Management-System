package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.dto.response.UserResponse;
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
        // Tìm user và userProfile
                User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null) {
            // Nếu userProfile chưa tồn tại thì tạo mới
            userProfile = userProfileMapper.toEntity(request);
        } else {
            // Nếu đã có thì update
            userProfileMapper.updateUserProfileFromDto(request, userProfile);
        }
        userProfile.setUser(user);
        userProfileRepository.save(userProfile);

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
        if (user.getUserProfile() != null) {
            userProfileRepository.delete(user.getUserProfile());
            user.setUserProfile(null);
            userRepository.save(user);
        }
        // Nếu muốn xóa luôn user thì có thể gọi userRepository.delete(user);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUserProfileByName(String name) {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (!user.getUserProfile().getFirstName().equalsIgnoreCase(name) &&
                    !user.getUserProfile().getLastName().equalsIgnoreCase(name)) {
                users.remove(user);
            }
        }
        return users.stream()
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
}