package com.dna_testing_system.dev.dto.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerFeedbackResponse {
    Long id;
    String customerName;
    String serviceName;
    Long orderId;
    String feedbackTitle;
    String feedbackContent;
    Float overallRating;
    Integer serviceQualityRating;
    Integer staffBehaviorRating;
    Integer timelinessRating;
    Boolean responseRequired;
    String responseContent;
    LocalDateTime respondedAt;
    String respondedByName;
    LocalDateTime createdAt;
}
