package com.G7.CTBS.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;
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
            throw new RuntimeException("Lỗi gửi mail: " + e.getMessage());
        }
    }}