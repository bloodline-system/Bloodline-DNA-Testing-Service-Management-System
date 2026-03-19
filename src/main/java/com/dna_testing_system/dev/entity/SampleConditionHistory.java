package com.dna_testing_system.dev.entity;

import com.dna_testing_system.dev.enums.SampleConditionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sample_condition_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleConditionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sample_id")
    private SampleCollection sample;

    @Enumerated(EnumType.STRING)
    private SampleConditionStatus status;

    private String conditionNote;

    private LocalDateTime timestamp;
}
