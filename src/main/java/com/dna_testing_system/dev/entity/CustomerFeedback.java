package com.dna_testing_system.dev.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Table(name = "tbl_customer_feedback")
public class CustomerFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id", nullable = false)
    Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    ServiceOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    MedicalService service;

    @Min(1) @Max(5)
    @Column(name = "overall_rating")
    Float overallRating;

    @Min(1) @Max(5)
    @Column(name = "service_quality_rating")
    Integer serviceQualityRating;

    @Min(1) @Max(5)
    @Column(name = "staff_behavior_rating")
    Integer staffBehaviorRating;

    @Min(1) @Max(5)
    @Column(name = "timeliness_rating")
    Integer timelinessRating;

    @Size(max = 255)
    @Column(name = "feedback_title")
    String feedbackTitle;

    @Lob
    @Column(name = "feedback_content")
    String feedbackContent;

    @Builder.Default
    @Column(name = "response_required", nullable = false)
    Boolean responseRequired = false;

    @Column(name = "responded_at")
    LocalDateTime respondedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by")
    User respondedBy;

    @Lob
    @Column(name = "response_content")
    String responseContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

}