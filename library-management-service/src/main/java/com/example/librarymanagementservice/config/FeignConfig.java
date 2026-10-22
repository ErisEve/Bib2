package com.example.librarymanagementservice.config;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(5000, 10000);  // Connection timeout, Read timeout
    }

    @Bean
    public RequestInterceptor internalServiceInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                template.header("X-Internal-Service", "true");
                // Or use a static token for internal communication
                template.header("Authorization", "Bearer internal-service-token");
            }
        };
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Add internal service header
                template.header("X-Internal-Service", "true");

                // Also forward the JWT token if available
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getCredentials() instanceof String) {
                    String token = (String) authentication.getCredentials();
                    if (token != null && !token.isEmpty()) {
                        template.header("Authorization", "Bearer " + token);
                    }
                }
            }
        };
    }

    
}