package com.dna_testing_system.dev.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateFeedbackRequest {
    @NotNull
    Long serviceId;
    Long orderId; // optional
    @NotNull
    String customerId;
//    @Min(1) @Max(5)
//    Float overallRating; // auto calc from 3 rating below
    @Min(1) @Max(5)
    Integer serviceQualityRating;
    @Min(1) @Max(5)
    Integer staffBehaviorRating;
    @Min(1) @Max(5)
    Integer timelinessRating;
    @Size(max = 255)
    String feedbackTitle;
    String feedbackContent;
    Boolean responseRequired = false;
}
