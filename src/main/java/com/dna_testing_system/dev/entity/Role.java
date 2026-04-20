package com.dna_testing_system.dev.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false)
    Long id;

    @Size(max = 100)
    @NotNull
    @Column(name = "role_name", nullable = false, length = 100)
    String roleName;

    @Size(max = 500)
    @Column(name = "role_description", length = 500)
    String roleDescription;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    Set<UserRole> userRoles = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    Set<RolePermission> rolePermissions = new HashSet<>();
}