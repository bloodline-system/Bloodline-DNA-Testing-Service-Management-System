package com.dna_testing_system.dev.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {
    
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName;
    
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dateOfBirth;
    
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[0-9\\+\\-\\s]*$", message = "Phone number must contain only digits, +, -, and spaces")
    String phoneNumber;
    
    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    String profileImageUrl;
    
    @Email(message = "Email must be valid")
    String email;
}
