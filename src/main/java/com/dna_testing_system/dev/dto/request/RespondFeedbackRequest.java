package com.dna_testing_system.dev.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RespondFeedbackRequest {
    String respondByUserId;
    @NotNull
    String responseContent;
}
