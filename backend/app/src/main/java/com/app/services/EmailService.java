package com.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.logging.models.LogContext;
import com.logging.services.LoggingService;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private LoggingService log;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@restaurant.local}")
    private String fromEmail;

    private LogContext getLogContext(String methodName) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .build();
    }

    @Async
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        LogContext logContext = getLogContext("sendVerificationEmail");
        String verifyLink = frontendUrl + "/verify?token=" + verificationToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Xác thực tài khoản - Bistro Restaurant");
        message.setText(
            "Chào bạn,\n\n"
            + "Cảm ơn bạn đã đăng ký tài khoản tại Bistro Restaurant.\n\n"
            + "Vui lòng click vào link sau để xác thực tài khoản:\n"
            + verifyLink + "\n\n"
            + "Link có hiệu lực trong 24 giờ.\n"
            + "Nếu bạn không đăng ký, vui lòng bỏ qua email này.\n\n"
            + "Trân trọng,\nBistro Restaurant"
        );

        try {
            mailSender.send(message);
            log.logInfo("Verification email sent to " + toEmail, logContext);
        } catch (MailException e) {
            log.logError("Failed to send verification email to " + toEmail, e, logContext);
        }
    }
}
