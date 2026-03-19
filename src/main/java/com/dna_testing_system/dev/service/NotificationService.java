package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.response.NotificationActivityResponse;
import com.dna_testing_system.dev.entity.Notification;
import com.dna_testing_system.dev.entity.User;

import java.util.List;

public interface NotificationService {

    void sendNotification(Notification notification);

    void sendNotification(NotificationActivityResponse notificationActivityResponse);

    Notification save(Notification notification);

    List<NotificationActivityResponse> getAllNotifications(User user);
    // Có thể thêm các phương thức khác như:
    // List<Notification> getUserNotifications(Long userId);
    // void markAsRead(Long notificationId);
}
