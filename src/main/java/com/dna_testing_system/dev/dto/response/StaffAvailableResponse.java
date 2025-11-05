package com.dna_testing_system.dev.dto.response;

import com.dna_testing_system.dev.entity.UserRole;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffAvailableResponse {
    String id;
    String username;
    String fullName;
    String profileImageUrl;
    String email;
    String phoneNumber;
    Set<String> roleName;
    Boolean isActive;
    LocalDateTime createdAt;
}
