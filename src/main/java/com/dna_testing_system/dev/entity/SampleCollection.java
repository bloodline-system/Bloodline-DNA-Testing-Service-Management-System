package com.dna_testing_system.dev.entity;

import com.dna_testing_system.dev.enums.CollectionStatus;
import com.dna_testing_system.dev.enums.SampleQuality;
import com.dna_testing_system.dev.enums.SampleType;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "tbl_sample_collections")
public class SampleCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "collection_id", nullable = false)
    Long collectionId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    @JoinColumn(name = "order_id", nullable = false)
    ServiceOrder order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    @JoinColumn(name = "staff_id", nullable = false)
    User staff;

    @NotNull
    @Column(name = "collection_date", nullable = false)
    LocalDateTime collectionDate;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "sample_quality", nullable = false, length = 50)
    SampleQuality sampleQuality;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "collection_status", nullable = false, length = 50)
    CollectionStatus collectionStatus = CollectionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "sample_type", nullable = false, length = 50)
    SampleType sampleType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "assigned_at")
    LocalDateTime assignAt;
}