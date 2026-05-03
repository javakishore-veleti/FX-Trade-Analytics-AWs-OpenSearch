package com.jk.fx.trade_mgmt.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the customer portal (and admin portal) to call this service cross-origin
 * during local dev when the region map points each region to localhost:8080.
 * Override <code>fx.cors.allowed-origins</code> per environment.
 */
@Configuration
@ConfigurationProperties(prefix = "fx.cors")
@Data
public class CorsConfig implements WebMvcConfigurer {

    private List<String> allowedOrigins = List.of();

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.isEmpty()) return;
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
