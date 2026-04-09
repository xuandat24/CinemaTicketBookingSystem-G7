package com.G7.CTBS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CtbsDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CtbsDemoApplication.class, args);
    }

}
