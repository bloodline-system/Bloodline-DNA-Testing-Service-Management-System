package com.dna_testing_system.dev.service.user.impl;


import com.dna_testing_system.dev.dto.request.UpdateProfileRequest;
import com.dna_testing_system.dev.dto.response.UserResponse;
import com.dna_testing_system.dev.entity.*;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.exception.AddRoleFailException;
import com.dna_testing_system.dev.exception.ResourceNotFoundException;
import com.dna_testing_system.dev.mapper.UserMapper;
import com.dna_testing_system.dev.mapper.UserProfileMapper;
import com.dna_testing_system.dev.repository.*;
import com.dna_testing_system.dev.service.user.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final TestResultRepository testResultRepository;

    @Override
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
    @Override
    public TestResult getTestResult(Long orderId) {
        List<TestResult> testResults = testResultRepository.findAll();
        for( TestResult testResult : testResults) {
            ServiceOrder serviceOrder = testResult.getOrder();
            if (serviceOrder.getId().equals(orderId)) {
                TestResult result = testResultRepository.findById(testResult.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Service order not found for ID: " + orderId));
                return result;
            }
        }
        return null;
    }

    @Override
    public void createUser(SignUp signUp) {
        log.info("Creating user with email: {}", signUp.getEmail());
        User userEntity = userMapper.toUser(signUp);
        userEntity.getProfile().setUser(userEntity);
        userEntity.setSignUp(signUp);
        userRepository.save(userEntity);
        assignRoles(userEntity, Set.of(RoleType.CUSTOMER));
        log.info("Created user with email: {}", signUp.getEmail());
    }

    @Override
    public User getUserByUserName(String userName) {
        return userRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + userName));
    }

    @Override
    @Transactional
    public User updateProfile(String userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        UserProfile profile = user.getProfile();

        if (profile == null) {
            // Create new profile if it doesn't exist
            profile = userProfileMapper.toUserProfile(request, user);
        } else {
            // Update existing profile using mapper
            userProfileMapper.updateUserProfileFromDto(request, profile);
        }

        userProfileRepository.save(profile);
        user.setProfile(profile);
        log.info("User Profile Updated Successfully");
        return user;
    }

    @Override
    public UserResponse toUserResponse(User user) {
        return userMapper.toResponse(user);
    }

    @Override
    public void updatePassword(String userName, String encodedPassword) {
        User user = getUserByUserName(userName);
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);
    }

    private void assignRoles(User user, Set<RoleType> roleTypes) {

        if (roleTypes == null || roleTypes.isEmpty()) {
            throw new AddRoleFailException("No roles provided for assignment");
        }
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            user.getUserRoles().clear();
        }

        for (RoleType roleType : roleTypes) {

            Role role = roleRepository.findRoleByRoleName(roleType.name())
                    .orElseThrow(
                            () -> new AddRoleFailException("Role not found: " + roleType.name())
                    );

            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);

            user.getUserRoles().add(userRole);
            roleRepository.save(role);
        }
    }
}
