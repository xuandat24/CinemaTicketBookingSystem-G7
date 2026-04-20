package com.G7.CTBS.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            // DÒNG QUAN TRỌNG: "Tên hiển thị" <email_gốc>
            helper.setFrom("Cinema Booking System-CTBS <khongcogi212@gmail.com>");

            helper.setTo(toEmail);
            helper.setSubject("OTP Code for Registration Confirmation - CTBS");

            // Bạn có thể dùng HTML để mail trông đẹp hơn
            String htmlContent = "<h3>Your verification code is: <b style='color:red;'>" + otp + "</b></h3>" +
                    "<p>This code is valid for 5 minutes.</p>";

            helper.setText(htmlContent, true); // true nghĩa là gửi dạng HTML

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error sending email : " + e.getMessage());
        }
    }

    // Hàm gửi thông báo chung (Giáng chức, Cảnh báo, v.v...)
    public void sendNotificationEmail(String toEmail, String subject, String messageContent) {
        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(messageContent);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Could not send notification email to " + toEmail + ": " + e.getMessage());
        }
    }
}