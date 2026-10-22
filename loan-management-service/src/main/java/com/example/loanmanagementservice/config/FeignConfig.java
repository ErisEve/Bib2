package com.example.loanmanagementservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                try {
                    // Get token from SecurityContext
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null && authentication.getCredentials() != null) {
                        String token = authentication.getCredentials().toString();
                        if (token != null && !token.isEmpty() && !token.equals("null")) {
                            template.header("Authorization", "Bearer " + token);
                            System.out.println("Added token to Feign request");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not add token to Feign request: " + e.getMessage());
                }
            }
        };
    }
}