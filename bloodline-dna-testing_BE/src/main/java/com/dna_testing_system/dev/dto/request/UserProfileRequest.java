package com.dna_testing_system.dev.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileRequest {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dateOfBirth;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[0-9\\+\\-\\s]*$", message = "Phone number must contain only digits, +, -, and spaces")
    String phoneNumber;


    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    String password;

    String profileImageUrl;
}
