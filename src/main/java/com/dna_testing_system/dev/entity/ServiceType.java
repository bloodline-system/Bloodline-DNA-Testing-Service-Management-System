package com.dna_testing_system.dev.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_service_types")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ServiceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_type_id")
    Long id;

    @NotBlank
    @Column(name = "type_name", nullable = false, unique = true, length = 100)
    String typeName;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @OneToMany(mappedBy = "serviceType", cascade = CascadeType.ALL, orphanRemoval = true)
    List<MedicalService> medicalServices;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
