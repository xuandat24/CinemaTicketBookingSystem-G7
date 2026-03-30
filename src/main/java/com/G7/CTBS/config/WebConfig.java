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
        Path bannerUploadDir = Paths.get("./uploads/banners");
        String bannerUploadPath = bannerUploadDir.toFile().getAbsolutePath();

        Path trailerUploadDir = Paths.get("./uploads/trailers");
        String trailerUploadPath = trailerUploadDir.toFile().getAbsolutePath();

        Path comboUploadDir = Paths.get("./uploads/combos");
        String comboUploadPath = comboUploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/banners/**")
                .addResourceLocations("file:/" + bannerUploadPath + "/");

        registry.addResourceHandler("/trailers/**")
                .addResourceLocations("file:/" + trailerUploadPath + "/");

        registry.addResourceHandler("/combos/**")
                .addResourceLocations("file:/" + comboUploadPath + "/");
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers((Connector connector) -> {
            connector.setMaxPostSize(500 * 1024 * 1024);
            connector.setProperty("maxSwallowSize", "-1");
            connector.setMaxParameterCount(10000);

            try {
                Method setMaxPartCount = connector.getClass().getMethod("setMaxPartCount", int.class);
                setMaxPartCount.invoke(connector, 1000);
            } catch (Exception e) {
                System.out.println(">> Tomcat Customizer: Cannot use setMaxPartCount in this version of Tomcat");
            }
        });
    }
}
