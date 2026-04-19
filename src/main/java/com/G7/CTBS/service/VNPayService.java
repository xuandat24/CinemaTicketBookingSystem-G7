package com.G7.CTBS.service;

import com.G7.CTBS.config.VNPayConfig;
import com.G7.CTBS.entity.Booking;
import com.G7.CTBS.entity.Payment;
import com.G7.CTBS.repository.BookingRepository;
import com.G7.CTBS.repository.CouponRepository;
import com.G7.CTBS.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    @Autowired
    private VNPayConfig vnPayConfig;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private CouponRepository couponRepository;

    public String createPaymentUrl(Long bookingId, String ipAddress) throws Exception {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking does not exist: " + bookingId));

        long amount = booking.getFinalPrice().longValue();
        String transactionRef = bookingId + "_" + System.currentTimeMillis();

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setTransactionRef(transactionRef);
        payment.setAmount((double) amount);
        paymentRepository.save(payment);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", VNPayConfig.VERSION);
        params.put("vnp_Command", VNPayConfig.COMMAND);
        params.put("vnp_TmnCode", vnPayConfig.tmnCode);
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_OrderInfo", "Ticket booking payment #" + bookingId);
        params.put("vnp_OrderType", VNPayConfig.ORDER_TYPE);
        params.put("vnp_Locale", VNPayConfig.LOCALE);
        params.put("vnp_ReturnUrl", vnPayConfig.returnUrl);
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        params.put("vnp_ExpireDate", getExpireDate(15));

        String queryString = buildQueryString(params);
        String secureHash = hmacSHA512(vnPayConfig.hashSecret, queryString);

        return vnPayConfig.paymentUrl + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    @Transactional
    public Payment handleReturn(Map<String, String> params) throws Exception {
        if (!validateSignature(params)) {
            throw new RuntimeException("Invalid VNPay signature");
        }

        String transactionRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionId = params.get("vnp_TransactionNo");
        String payDateStr = params.get("vnp_PayDate");

        Payment payment = paymentRepository.findFirstByTransactionRefOrderByCreatedAtDesc(transactionRef)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + transactionRef));

        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            return payment;
        }

        payment.setTransactionId(transactionId);
        payment.setResponseCode(responseCode);
        payment.setPaymentTime(parsePayDate(payDateStr));

        if ("00".equals(responseCode)) {
            payment.setPaymentStatus("SUCCESS");

            Booking booking = payment.getBooking();
            booking.setStatus("CONFIRMED");
            bookingRepository.save(booking);
            return paymentRepository.save(payment);
        }

        payment.setPaymentStatus("FAILED");
        paymentRepository.save(payment);

        // VNPay responseCode 24 = user canceled payment.
        // Remove unfinished booking so My Tickets only keeps successful bills.
        if ("24".equals(responseCode)) {
            deleteBookingWithPayments(payment.getBooking());
        }

        return payment;
    }

    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findFirstByBooking_BookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new RuntimeException("No payment found for booking: " + bookingId));
    }

    public boolean validateSignature(Map<String, String> params) throws Exception {
        String receivedHash = params.get("vnp_SecureHash");
        Map<String, String> filtered = new TreeMap<>(params);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");
        String queryString = buildQueryString(filtered);
        String calculatedHash = hmacSHA512(vnPayConfig.hashSecret, queryString);
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    public void deleteBookingWithPayments(Booking booking) {
        if (booking == null || booking.getBookingId() == null) {
            return;
        }

        // 1. BẢO MẬT: HOÀN LẠI LƯỢT DÙNG COUPON NẾU ĐƠN BỊ HỦY
        if (booking.getCouponCode() != null) {
            couponRepository.findByCode(booking.getCouponCode()).ifPresent(coupon -> {
                if (coupon.getUsedCount() != null && coupon.getUsedCount() > 0) {
                    coupon.setUsedCount(coupon.getUsedCount() - 1);
                    couponRepository.save(coupon);
                }
            });
        }

        // 2. Tiến hành xóa Payment và Booking
        Long bookingId = booking.getBookingId();
        List<Payment> payments = paymentRepository.findAllByBooking_BookingIdOrderByCreatedAtDesc(bookingId);
        if (!payments.isEmpty()) {
            paymentRepository.deleteAll(payments);
        }
        bookingRepository.deleteById(bookingId);
    }

    private String buildQueryString(Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String getExpireDate(int minutes) {
        java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        cal.add(java.util.Calendar.MINUTE, minutes);
        return new SimpleDateFormat("yyyyMMddHHmmss").format(cal.getTime());
    }

    private java.time.LocalDateTime parsePayDate(String payDate) {
        try {
            return java.time.LocalDateTime.parse(payDate,
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            return java.time.LocalDateTime.now();
        }
    }
}
