package com.G7.CTBS.config;

import jakarta.servlet.MultipartConfigElement;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Method;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cấp phép cho đường dẫn /banners/** truy cập vào thư mục vật lý uploads/banners/
        registry.addResourceHandler("/banners/**")
                .addResourceLocations("file:uploads/banners/");
        
        // Cấp phép cho đường dẫn /trailers/** truy cập vào thư mục vật lý uploads/trailers/
        registry.addResourceHandler("/trailers/**")
                .addResourceLocations("file:uploads/trailers/");
    }
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers((Connector connector) -> {
                
                // 1. Mở khóa dung lượng nuốt dữ liệu và kích thước POST
                connector.setMaxPostSize(500 * 1024 * 1024); // 500 MB
                connector.setProperty("maxSwallowSize", "-1");
                
                // 2. Mở khóa số lượng tham số (Parameters)
                connector.setMaxParameterCount(10000);
                
                // 3. Mở khóa số lượng File/Part (Chữa dứt điểm lỗi FileCountLimitExceededException)
                // Sử dụng Reflection để vượt qua bài kiểm tra lỗi Cú pháp (Cannot find symbol) của IDE
                try {
                    Method setMaxPartCount = connector.getClass().getMethod("setMaxPartCount", int.class);
                    setMaxPartCount.invoke(connector, 1000); // Ép Tomcat cho phép 1000 parts thay vì 50
                } catch (Exception e) {
                    System.out.println(">> Tomcat Customizer: Cannot use setMaxPartCount in this version of Tomcat");
                }
            });
        };
    }
}