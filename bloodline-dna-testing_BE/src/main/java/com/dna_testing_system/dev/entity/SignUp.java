package com.dna_testing_system.dev.entity;


import com.dna_testing_system.dev.enums.SignUpStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for tracking user signup process with email verification.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_sign_up")
public class SignUp {

    @Id
    @Column(name = "signup_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false, length = 50)
    String username;

    @Column(nullable = false, columnDefinition = "TEXT")
    String password;

    @Size(max = 100)
    @Column(name = "first_name", length = 100)
    String firstName;

    @Size(max = 100)
    @Column(name = "last_name", length = 100)
    String lastName;

    @Column(nullable = false, unique = true, length = 100)
    String email;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    SignUpStatus status;

    @Column(nullable = false, length = 6)
    String currentVerificationToken;

    @Column(nullable = false)
    LocalDateTime expiredVerificationTokenDate;

    @Column(nullable = false)
    Integer countAttemptVerificationToken;

    @Column(nullable = false)
    LocalDateTime lastVerificationTokenSentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
