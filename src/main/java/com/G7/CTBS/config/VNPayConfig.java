package com.G7.CTBS.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class VNPayConfig {

    @Value("${vnpay.tmn-code}")
    public String tmnCode;

    @Value("${vnpay.hash-secret}")
    public String hashSecret;

    @Value("${vnpay.payment-url}")
    public String paymentUrl;

    @Value("${vnpay.return-url}")
    public String returnUrl;

    public static final String VERSION = "2.1.0";
    public static final String COMMAND = "pay";
    public static final String ORDER_TYPE = "other";
    public static final String LOCALE = "en";
    public static final String CURRENCY_CODE = "VND";
}
