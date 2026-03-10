package com.dna_testing_system.dev.dto.response;


import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.NotificationCategory;
import com.dna_testing_system.dev.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationActivityResponse {

    Long notificationId;

    User recipientUser;

    NotificationType notificationType;

    NotificationCategory notificationCategory;

    String subject;

    String messageContent;

    LocalDateTime createdAt;
}
