package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.UpdateProfileRequest;
import com.dna_testing_system.dev.dto.response.UserResponse;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.ResourceNotFoundException;
import com.dna_testing_system.dev.mapper.UserMapper;
import com.dna_testing_system.dev.repository.OrderServiceRepository;
import com.dna_testing_system.dev.repository.TestResultRepository;
import com.dna_testing_system.dev.repository.UserProfileRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    UserMapper userMapper;
    OrderServiceRepository orderServiceRepository;
    TestResultRepository testResultRepository;

    @Override
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
    }

    @Override
    @Transactional
    public User updateProfile(String userId, UpdateProfileRequest request) {
        User user = getUserById(userId);
        
        UserProfile profile = user.getUserProfile();
        
        if (profile == null) {
            // Create new profile if it doesn't exist
            profile = userMapper.toUserProfile(request, user);
        } else {
            // Update existing profile using mapper
            userMapper.updateUserProfileFromDto(request, profile);
        }
        
        userProfileRepository.save(profile);
        user.setUserProfile(profile);
        log.info("User Profile Updated Successfully");
        return user;
    }
    
    @Override
    public UserResponse toUserResponse(User user) {
        return userMapper.toResponse(user);
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
}
