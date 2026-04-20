package com.G7.CTBS.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối cho banners
        Path bannerUploadDir = Paths.get("./uploads/banners");
        String bannerUploadPath = bannerUploadDir.toFile().getAbsolutePath();

        // Lấy đường dẫn tuyệt đối cho trailers
        Path trailerUploadDir = Paths.get("./uploads/trailers");
        String trailerUploadPath = trailerUploadDir.toFile().getAbsolutePath();

        // ĐÃ SỬA: Lấy đường dẫn tuyệt đối chuẩn xác cho avatars
        Path avatarUploadDir = Paths.get("./uploads/avatars");
        String avatarUploadPath = avatarUploadDir.toFile().getAbsolutePath();

        Path comboUploadDir = Paths.get("./uploads/combos");
        String comboUploadPath = comboUploadDir.toFile().getAbsolutePath();

        // Cấp quyền truy cập các thư mục
        registry.addResourceHandler("/banners/**").addResourceLocations("file:/" + bannerUploadPath + "/");

        registry.addResourceHandler("/trailers/**").addResourceLocations("file:/" + trailerUploadPath + "/");

        // ĐÃ SỬA: Ánh xạ chuẩn bằng đường dẫn tuyệt đối
        registry.addResourceHandler("/avatars/**").addResourceLocations("file:/" + avatarUploadPath + "/");

        registry.addResourceHandler("/combos/**").addResourceLocations("file:/" + comboUploadPath + "/");

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

                // 3. Mở khóa số lượng File/Part
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