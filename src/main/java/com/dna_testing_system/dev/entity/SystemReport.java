package com.dna_testing_system.dev.entity;

import com.dna_testing_system.dev.enums.ReportStatus;
import com.dna_testing_system.dev.enums.ReportType;
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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_system_reports")
public class SystemReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false)
    Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "report_name", nullable = false)
    String reportName;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "report_type", nullable = false, length = 100)
    ReportType reportType;

    @Size(max = 100)
    @NotNull
    @Column(name = "report_category", nullable = false, length = 100)
    String reportCategory;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by_user_id", nullable = false)
    User generatedByUser;

    @NotNull
    @Lob
    @Column(name = "report_data", nullable = false)
    String reportData;

    @Size(max = 1000)
    @Column(name = "report_file_path", length = 1000)
    String reportFilePath;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "report_status", nullable = false, length = 50)
    ReportStatus reportStatus = ReportStatus.GENERATED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

}