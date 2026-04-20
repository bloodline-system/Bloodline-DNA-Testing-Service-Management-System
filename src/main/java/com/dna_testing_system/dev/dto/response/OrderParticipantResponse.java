package com.dna_testing_system.dev.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderParticipantResponse {
    String firstName;
    String lastName;
    String gender;
    LocalDate dateBirth;
}
