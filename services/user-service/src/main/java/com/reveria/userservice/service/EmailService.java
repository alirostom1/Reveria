package com.reveria.userservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.name:Reveria}")
    private String appName;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.mail.from:noreply@reveria.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Reveria}")
    private String fromName;


    @Async
    public void sendPasswordResetEmail(String to, String username, String token, int expirationHours) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        Context context = new Context();
        context.setVariable("appName", appName);
        context.setVariable("username", username);
        context.setVariable("resetLink", resetLink);
        context.setVariable("expirationHours", expirationHours);

        String html = templateEngine.process("email/password-reset", context);

        sendHtmlEmail(to, "Reset Your Password - " + appName, html);
    }


    @Async
    public void sendEmailVerification(String to, String username, String token, int expirationHours) {
        String verifyLink = frontendUrl + "/verify-email?token=" + token;

        Context context = new Context();
        context.setVariable("appName", appName);
        context.setVariable("username", username);
        context.setVariable("verifyLink", verifyLink);
        context.setVariable("expirationHours", expirationHours);

        String html = templateEngine.process("email/email-verification", context);

        sendHtmlEmail(to, "Verify Your Email - " + appName, html);
    }


    @Async
    public void sendWelcomeEmail(String to, String username) {
        String loginLink = frontendUrl + "/login";

        Context context = new Context();
        context.setVariable("appName", appName);
        context.setVariable("username", username);
        context.setVariable("loginLink", loginLink);

        String html = templateEngine.process("email/welcome", context);

        sendHtmlEmail(to, "Welcome to " + appName + "!", html);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
}