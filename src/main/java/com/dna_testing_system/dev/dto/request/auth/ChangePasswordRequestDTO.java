package com.dna_testing_system.dev.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequestDTO {

    @NotBlank(message = "Current password is required")
    @Size(min = 8, max = 50, message = "Current password must be between 8 and 50 characters")
    String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 50, message = "New password must be between 8 and 50 characters")
    String newPassword;

    @NotBlank(message = "Confirm password is required")
    @Size(min = 8, max = 50, message = "Confirm password must be between 8 and 50 characters")
    String confirmPassword;
}