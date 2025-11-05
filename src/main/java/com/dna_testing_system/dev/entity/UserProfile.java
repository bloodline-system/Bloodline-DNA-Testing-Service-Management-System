package com.dna_testing_system.dev.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_user_profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_id", nullable = false)
    Long profileId;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Size(max = 100)
    @Column(name = "first_name", length = 100)
    String firstName;

    @Size(max = 100)
    @Column(name = "last_name", length = 100)
    String lastName;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Size(max = 500)
    @Column(name = "profile_image_url", length = 500)
    String profileImageUrl;

    @Size(max = 100)
    @NotNull
    @Column(name = "email", nullable = false)
    String email;

    @Size(max = 10)
    @Column(name = "phone_number", length = 20)
    String phoneNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

}