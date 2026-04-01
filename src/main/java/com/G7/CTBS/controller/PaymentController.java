package com.G7.CTBS.controller;

import com.G7.CTBS.dto.BookingRequestDTO;
import com.G7.CTBS.entity.Payment;
import com.G7.CTBS.service.BookingService;
import com.G7.CTBS.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired private BookingService bookingService;
    @Autowired private VNPayService vnPayService;

    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(
            @RequestBody BookingRequestDTO request,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();
        try {
            Long bookingId = bookingService.createBooking(request);

            String ip = getClientIp(httpRequest);
            String paymentUrl = vnPayService.createPaymentUrl(bookingId, ip);

            response.put("success", true);
            response.put("paymentUrl", paymentUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(
            @RequestParam Map<String, String> params,
            HttpServletResponse response) throws IOException {

        try {
            vnPayService.handleReturn(params);
        } catch (Exception e) {
            System.err.println("VNPay return error: " + e.getMessage());
        }

        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        if ("24".equals(responseCode)) {
            response.sendRedirect("/?payment=cancelled");
            return;
        }

        response.sendRedirect("/booking/history");
    }

    @GetMapping("/status/{bookingId}")
    public ResponseEntity<?> checkStatus(@PathVariable Long bookingId) {
        try {
            Payment payment = vnPayService.getPaymentByBookingId(bookingId);
            Map<String, Object> response = new HashMap<>();
            response.put("bookingId", bookingId);
            response.put("paymentStatus", payment.getPaymentStatus());
            response.put("amount", payment.getAmount());
            response.put("transactionId",
                    payment.getTransactionId() != null ? payment.getTransactionId() : "N/A");
            response.put("paymentTime",
                    payment.getPaymentTime() != null ? payment.getPaymentTime().toString() : "N/A");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip.split(",")[0].trim();
    }
}
