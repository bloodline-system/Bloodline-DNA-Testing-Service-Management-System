package com.dna_testing_system.dev.template;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class CompleteUserMailHandler extends AbstractMailHandler {

    public CompleteUserMailHandler(JavaMailSender mailSender, SpringTemplateEngine templateEngine, Map<String, Object> variables, String senderMail, String... recipientMails) {
        super(mailSender, templateEngine, senderMail, recipientMails, variables);
    }

    @Override
    public String getMailSubject() {
        return "Complete to create user";
    }

    @Override
    public String getTemplate() {
        return "complete-user-mail-template";
    }

    @Override
    public Charset getCharset() {
        return StandardCharsets.UTF_8;
    }
}
