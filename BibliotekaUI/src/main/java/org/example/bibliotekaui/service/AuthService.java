package org.example.bibliotekaui.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bibliotekaui.feign_client.AuthClient;
import org.example.bibliotekaui.dto.AuthRequest;
import org.example.bibliotekaui.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthClient authClient;

    /**
     * Authenticate user with email and password
     */
    public AuthResponse login(String email, String password) {
        log.info("Attempting login for user: {}", email);

        try {
            AuthRequest request = new AuthRequest(email, password);
            AuthResponse response = authClient.login(request);
            log.info("Login successful for user: {}", email);
            return response;
        } catch (Exception e) {
            log.error("Login failed for user: {} - {}", email, e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Register a new user
     */
    public AuthResponse register(String email, String password, String role) {
        log.info("Attempting registration for user: {}", email);

        try {
            AuthRequest request = new AuthRequest(email, password, role);
            AuthResponse response = authClient.register(request);
            log.info("Registration successful for user: {}", email);
            return response;
        } catch (Exception e) {
            log.error("Registration failed for user: {} - {}", email, e.getMessage());
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate token (optional)
     */
    public boolean validateToken(String token) {
        try {
            // You could add a validate endpoint in your auth service
            // return authClient.validateToken(token);
            return token != null && !token.isEmpty();
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}