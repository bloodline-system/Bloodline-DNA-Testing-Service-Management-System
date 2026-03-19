package com.dna_testing_system.dev.service.mail;


import com.dna_testing_system.dev.event.type.NotificationEvent;

public interface MailService {

    void sendVerifyUserMail(NotificationEvent verifyUserMailEvent);

    void sendCompleteUserMail(NotificationEvent completeUserMailEvent);
}
