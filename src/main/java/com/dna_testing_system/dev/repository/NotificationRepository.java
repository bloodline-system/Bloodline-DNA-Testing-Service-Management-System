package com.dna_testing_system.dev.repository;

import com.dna_testing_system.dev.entity.Notification;
import com.dna_testing_system.dev.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findAllByRecipientUserOrderByCreatedAtDesc(User recipientUser);
}
