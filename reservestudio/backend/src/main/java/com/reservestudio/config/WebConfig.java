package com.reservestudio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String[] allowedOriginPatterns;

    public WebConfig(AuthInterceptor authInterceptor,
                     @Value("${app.cors.allowed-origin-patterns}") String[] allowedOriginPatterns) {
        this.authInterceptor = authInterceptor;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/bookings/**", "/api/auth/me");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns) // credentials ile joker port desteği için pattern
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowCredentials(true); // oturum cookie'si için
    }
}
