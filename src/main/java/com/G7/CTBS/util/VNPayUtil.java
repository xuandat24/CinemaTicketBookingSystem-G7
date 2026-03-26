package com.G7.CTBS.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class VNPayUtil {

    /**
     * Generate HMAC SHA512 signature for VNPay
     */
    public static String generateSignature(String data, String secretKey) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKeySpec);
            byte[] hmacBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error generating signature: ", e);
            return "";
        }
    }

    /**
     * Build query string (URL-encoded) from sorted params.
     * FIX: dùng StringJoiner để tránh lỗi trailing '&' khi value rỗng.
     */
    public static String buildQuery(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringJoiner query = new StringJoiner("&");
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                query.add(URLEncoder.encode(fieldName, StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
            }
        }
        return query.toString();
    }

    /**
     * Build hash data string from sorted params for signature computation.
     * FIX: dùng StringJoiner để tránh lỗi trailing '&' khi value rỗng.
     * NOTE: For VNPay signature the hash data must use raw parameter values (no URL encoding).
     */
    public static String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringJoiner hashData = new StringJoiner("&");
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Use raw value (no encoding) when building hash data
                hashData.add(fieldName + "=" + fieldValue);
            }
        }
        return hashData.toString();
    }

    /**
     * URL encode string (spaces → %20, not +)
     */
    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            log.error("Error encoding url: ", e);
            return str;
        }
    }

    /**
     * URL decode string
     */
    public static String urlDecode(String str) {
        try {
            return java.net.URLDecoder.decode(str, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decoding url: ", e);
            return str;
        }
    }

    /**
     * Get client IP address from request.
     * FIX: lấy IP đầu tiên trong X-Forwarded-For để tránh header giả mạo.
     */
    public static String getIpAddress(HttpServletRequest request) {
        try {
            String xff = request.getHeader("X-FORWARDED-FOR");
            if (xff != null && !xff.isEmpty()) {
                // Lấy IP đầu tiên (IP gốc của client), bỏ các proxy trung gian
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.error("Error getting IP address: ", e);
            return "unknown";
        }
    }

    /**
     * Verify VNPay response signature
     */
    public static boolean verifySignature(String inputHash, String data, String secretKey) {
        String computedHash = generateSignature(data, secretKey);
        return computedHash.equalsIgnoreCase(inputHash);
    }
}