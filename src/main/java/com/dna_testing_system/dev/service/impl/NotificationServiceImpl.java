package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.response.NotificationActivityResponse;
import com.dna_testing_system.dev.entity.Notification;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.repository.NotificationRepository;
import com.dna_testing_system.dev.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationServiceImpl implements NotificationService {

    SimpMessagingTemplate messagingTemplate;
    NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public List<NotificationActivityResponse> getAllNotifications(User user) {
        var notification = notificationRepository.findAllByRecipientUserOrderByCreatedAtDesc(user);
        log.warn("find notifications by recipient user");
        return notification.stream()
                .map(n -> NotificationActivityResponse.builder()
                        .notificationId(n.getNotificationId())
                        .notificationType(n.getNotificationType())
                        .notificationCategory(n.getNotificationCategory())
                        .subject(n.getSubject())
                        .messageContent(n.getMessageContent())
                        .createdAt(n.getCreatedAt())
                        .build())

                .collect(Collectors.toList());
    }

    @Override
    public void sendNotification(Notification notification) {
        String username = notification.getRecipientUser().getUsername();
        String destination = "/user/" + username + "/queue/notifications";
        log.info("Sending WS notification to {} with payload: {}", username, notification);

        messagingTemplate.convertAndSend(
                destination
                , notification);
//        String username = notification.getRecipientUser().getUsername();
//        String destination = "/queue/notifications";
//        log.info("Sending WS notification to {} with payload: {}", username, notification);
//        messagingTemplate.convertAndSendToUser(username, destination, notification);
    }

    @Override
    public void sendNotification(NotificationActivityResponse notificationActivityResponse) {

    }
}
