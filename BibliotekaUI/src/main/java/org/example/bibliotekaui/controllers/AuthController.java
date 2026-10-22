package org.example.bibliotekaui.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bibliotekaui.dto.AuthRequest;
import org.example.bibliotekaui.dto.AuthResponse;
import org.example.bibliotekaui.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("pageStyle", "login");
        return "app";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageStyle", "home");
        return "app";
    }

    /**
     * REST endpoint for login (called by frontend JavaScript)
     */
    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            log.info("Login request received for: {}", authRequest.getEmail());
            AuthResponse response = authService.login(
                    authRequest.getEmail(),
                    authRequest.getPassword()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.status(401).body("Invalid email or password");
        }
    }

    /**
     * REST endpoint for registration (called by frontend JavaScript)
     */
    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<?> register(@RequestBody AuthRequest authRequest) {
        try {
            log.info("Registration request received for: {}", authRequest.getEmail());
            AuthResponse response = authService.register(
                    authRequest.getEmail(),
                    authRequest.getPassword(),
                    authRequest.getRole() != null ? authRequest.getRole() : "USER"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.status(400).body("Registration failed: " + e.getMessage());
        }
    }
}
