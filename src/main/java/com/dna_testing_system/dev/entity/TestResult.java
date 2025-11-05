package com.dna_testing_system.dev.entity;

import com.dna_testing_system.dev.enums.ResultStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_test_results")
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id", nullable = false)
    Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    ServiceOrder order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_data_id")
    RawTestData rawData;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analyzed_by_staff_id", nullable = false)
    User analyzedByStaff;

    @Column(name = "test_date", nullable = true)
    LocalDateTime testDate;

    @Lob
    @Column(name = "result_summary", nullable = true)
    String resultSummary;

    @Lob
    @Column(name = "detailed_results")
    String detailedResults;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "result_status", nullable = false, length = 50)
    ResultStatus resultStatus = ResultStatus.PENDING;

    @Builder.Default
    @Column(name = "report_generated", nullable = false)
    Boolean reportGenerated = false;

    @Size(max = 1000)
    @Column(name = "report_file_path", length = 1000)
    String reportFilePath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

}