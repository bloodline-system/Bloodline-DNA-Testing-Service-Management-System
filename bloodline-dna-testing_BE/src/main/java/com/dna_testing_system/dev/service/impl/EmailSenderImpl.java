package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.entity.SampleCollection;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.service.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailSenderImpl implements EmailSender {

    final JavaMailSender mailSender;
    final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    String fromEmail;

    @Value("${application.base-url}")
    String baseUrl;

    @Override
    public void sendNewOrderNotificationToManager(ServiceOrder order, String managerEmail) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("order", order);
        templateModel.put("timestamp", new Date());
        templateModel.put("baseUrl", baseUrl);

            sendEmailWithTemplate(
                    managerEmail,
                    "New DNA Testing Kit Order #" + order.getId(),
                    "manager-order-notification",
                    templateModel
            );
    }

    @Override
    public void sendTestAssignmentNotification(ServiceOrder order, SampleCollection sampleCollection, User staffMember) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("order", order);
        templateModel.put("sampleCollection", sampleCollection);
        templateModel.put("staffMember", staffMember);
        templateModel.put("timestamp", new Date());
        templateModel.put("baseUrl", baseUrl);


        sendEmailWithTemplate(
                staffMember.getUserProfile().getEmail(),
                "New DNA Test Assignment: Order #" + order.getId(),
                "test-assignment-notification",
                templateModel
        );
    }

    @Override
    public void sendTestAssignmentNotification(ServiceOrder order, TestResult testResult, User staffMember) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("order", order);
        templateModel.put("testResult", testResult);
        templateModel.put("staffMember", staffMember);
        templateModel.put("timestamp", new Date());
        templateModel.put("baseUrl", baseUrl);


        sendEmailWithTemplate(
                staffMember.getUserProfile().getEmail(),
                "New DNA Test Assignment: Order #" + order.getId(),
                "test-result-assignment-notification",
                templateModel
        );
    }

    @Override
    public void sendOrderStatusUpdateNotification(ServiceOrder order, String recipient) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("order", order);
        templateModel.put("timestamp", new Date());
        templateModel.put("baseUrl", baseUrl);

        String subject = "Order Status Update - Order #" + order.getId();
        String template = "order-status-update-notification";

        sendEmailWithTemplate(recipient, subject, template, templateModel);
    }


    private boolean sendEmailWithTemplate(String to, String subject, String template, Map<String, Object> templateModel) {
        try {
            final Context context = new Context();
            context.setVariables(templateModel);
            final String htmlContent = templateEngine.process("email/" + template, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(htmlContent, true);

            // Send email
            mailSender.send(mimeMessage);
            log.info("Email sent successfully to: " + to);
            return true;

        } catch (MessagingException e) {
            log.error( "Failed to send email to " + to, e);
            return false;
        }
    }
}