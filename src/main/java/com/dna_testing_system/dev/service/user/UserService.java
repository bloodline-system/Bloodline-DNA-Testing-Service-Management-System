package com.dna_testing_system.dev.service.user;

import com.dna_testing_system.dev.dto.request.UpdateProfileRequest;
import com.dna_testing_system.dev.dto.response.UserResponse;
import com.dna_testing_system.dev.entity.SignUp;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.entity.User;

public interface UserService {
    User getUserById(String userId);
    TestResult getTestResult(Long orderId);
    void createUser(SignUp signUp);
    User getUserByUserName(String userName);
    UserResponse toUserResponse(User user);
    User updateProfile(String userId, UpdateProfileRequest request);
}
