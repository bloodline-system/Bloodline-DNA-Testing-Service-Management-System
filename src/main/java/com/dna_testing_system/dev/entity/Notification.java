package com.dna_testing_system.dev.entity;

import com.dna_testing_system.dev.enums.NotificationCategory;
import com.dna_testing_system.dev.enums.NotificationType;
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
@Table(name = "tbl_notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    Long notificationId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    User recipientUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id")
    TestResult result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    ServiceOrder order;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "notification_type", nullable = false, length = 50)
    NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "notification_category", nullable = false, length = 50)
    NotificationCategory notificationCategory;

    @Size(max = 255)
    @NotNull
    @Column(name = "subject", nullable = false)
    String subject;

    @NotNull
    @Lob
    @Column(name = "message_content", nullable = false)
    String messageContent;

    @Size(max = 255)
    @Column(name = "recipient_email")
    String recipientEmail;

    @Column(name = "sent_at")
    LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

}