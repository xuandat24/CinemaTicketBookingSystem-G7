package com.G7.CTBS.controller;

import com.G7.CTBS.dto.BookingRequestDTO;
import com.G7.CTBS.entity.Booking;
import com.G7.CTBS.entity.Payment;
import com.G7.CTBS.entity.User;
import com.G7.CTBS.repository.BookingRepository;
import com.G7.CTBS.repository.PaymentRepository;
import com.G7.CTBS.service.BookingService;
import com.G7.CTBS.service.UserService;
import com.G7.CTBS.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired private BookingService bookingService;
    @Autowired private VNPayService vnPayService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserService userService;

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(
            @RequestBody BookingRequestDTO request,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();
        try {
            User user = resolveCurrentUser(httpRequest);
            if (user == null) {
                response.put("success", false);
                response.put("message", "User not logged in or invalid token.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            request.setUserId(user.getUserId());
            Long bookingId = bookingService.createBooking(request);

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Khong tim thay thong tin dat ve!"));

            if (booking.getFinalPrice() < 10000) {
                booking.setStatus("SUCCESS");
                bookingRepository.save(booking);

                Payment payment = new Payment();
                payment.setBooking(booking);
                payment.setAmount(booking.getFinalPrice());
                payment.setPaymentStatus("SUCCESS");
                payment.setProvider("SYSTEM_COUPON");
                payment.setPaymentTime(LocalDateTime.now());
                payment.setTransactionRef(bookingId + "_FREE_" + System.currentTimeMillis());
                paymentRepository.save(payment);

                response.put("success", true);
                response.put("paymentUrl", "/booking/history");
                return ResponseEntity.ok(response);
            }

            String ip = getClientIp(httpRequest);
            // VNPayService is the single source that creates/stores a payment row.
            String paymentUrl = vnPayService.createPaymentUrl(bookingId, ip);

            response.put("success", true);
            response.put("paymentUrl", paymentUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(
            @RequestParam Map<String, String> params,
            HttpServletResponse responseHttp) throws IOException {

        String responseCode = params.getOrDefault("vnp_ResponseCode", "");

        if ("24".equals(responseCode)) {
            responseHttp.sendRedirect("/?payment=cancelled");
            return;
        }

        try {
            vnPayService.handleReturn(params);
        } catch (Exception ignored) {
        }

        if ("00".equals(responseCode)) {
            String transactionRef = params.get("vnp_TxnRef");
            Payment payment = paymentRepository
                    .findFirstByTransactionRefOrderByCreatedAtDesc(transactionRef)
                    .orElse(null);

            if (payment != null && payment.getBooking() != null) {
                Booking booking = payment.getBooking();
                booking.setStatus("SUCCESS");
                bookingRepository.save(booking);

                payment.setPaymentStatus("SUCCESS");
                payment.setPaymentTime(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        }

        responseHttp.sendRedirect("/booking/history");
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        Map<String, String> response = new HashMap<>();
        try {
            if (!vnPayService.validateSignature(params)) {
                response.put("RspCode", "97");
                return ResponseEntity.ok(response);
            }

            String transactionRef = params.get("vnp_TxnRef");
            Payment payment = paymentRepository
                    .findFirstByTransactionRefOrderByCreatedAtDesc(transactionRef)
                    .orElse(null);

            if (payment == null) {
                response.put("RspCode", "01");
                return ResponseEntity.ok(response);
            }

            if ("SUCCESS".equals(payment.getPaymentStatus())) {
                response.put("RspCode", "02");
                return ResponseEntity.ok(response);
            }

            try {
                vnPayService.handleReturn(params);
            } catch (Exception ignored) {
            }

            if ("00".equals(params.get("vnp_ResponseCode"))) {
                Booking booking = payment.getBooking();
                if (booking != null) {
                    booking.setStatus("SUCCESS");
                    bookingRepository.save(booking);
                }
                payment.setPaymentStatus("SUCCESS");
                payment.setPaymentTime(LocalDateTime.now());
                paymentRepository.save(payment);
            }

            response.put("RspCode", "00");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("RspCode", "99");
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/status/{bookingId}")
    public ResponseEntity<?> checkStatus(@PathVariable Long bookingId) {
        try {
            Payment payment = vnPayService.getPaymentByBookingId(bookingId);
            return ResponseEntity.ok(Map.of(
                    "paymentStatus", payment.getPaymentStatus(),
                    "amount", payment.getAmount()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip.split(",")[0].trim();
    }

    private User resolveCurrentUser(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            User user = userService.findByUsernameOrEmail(auth.getName());
            if (user != null) return user;
        }

        Object principal = auth != null ? auth.getPrincipal() : null;
        if (principal instanceof OAuth2User oauth) {
            String email = oauth.getAttribute("email");
            if (email != null) return userService.findByUsernameOrEmail(email);
        }

        if (request != null && request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("username".equals(cookie.getName())) {
                    try {
                        String decodedUser = java.net.URLDecoder.decode(
                                cookie.getValue(),
                                java.nio.charset.StandardCharsets.UTF_8.name()
                        );
                        User user = userService.findByUsernameOrEmail(decodedUser);
                        if (user != null) return user;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }
}
