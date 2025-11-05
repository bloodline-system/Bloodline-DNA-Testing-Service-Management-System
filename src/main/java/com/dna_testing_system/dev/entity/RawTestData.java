package com.dna_testing_system.dev.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_raw_test_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RawTestData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_data_id", nullable = false)
    Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "data_format", nullable = false, length = 50)
    String dataFormat;

    @NotNull
    @Lob
    @Column(name = "raw_data_content", nullable = false)
    String rawDataContent;

    @Size(max = 1000)
    @Column(name = "file_path", length = 1000)
    String filePath;

    @NotNull
    @Column(name = "collected_at", nullable = false)
    LocalDateTime collectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "created_by")
    LocalDateTime createdBy;

}