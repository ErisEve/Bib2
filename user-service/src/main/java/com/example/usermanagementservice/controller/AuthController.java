package com.example.usermanagementservice.controller;


import com.example.usermanagementservice.dto.AuthRequest;
import com.example.usermanagementservice.dto.AuthResponse;
import com.example.usermanagementservice.model.User;
import com.example.usermanagementservice.repository.UserRepository;
import com.example.usermanagementservice.security.CustomUserDetailsService;
import com.example.usermanagementservice.security.JwtUtil;
import com.example.usermanagementservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:8081", "http://localhost:8083"})
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private UserService userService;

    // POST - Register
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        // Check if email already exists
        if(userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        // Default role is USER and creates an immutable Set with just one item ("ROLE_USER")
        user.setRole("ROLE_USER");

        // Register user (password in hashed in UserService)
        userService.registerUser(user);

        return ResponseEntity.ok("User registered successfully");
    }


    @Operation(
            summary = "Login user",
            description = "Authenticate a user and return a JWT token",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Login Example",
                                    summary = "Login using email and password",
                                    value = "{ \"email\": \"newuser112@example.com\", \"password\": \"StrongPass123\" }"
                            )
                    )
            )
    )
    // POST - Login
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody AuthRequest loginRequest) {
        System.out.println("Login attempt for email: " + loginRequest.getEmail());

        try{
            // Authenticate user credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest
                            .getEmail(), loginRequest.getPassword())
            );
            System.out.println("AuthenticationManager authenticate() succeeded");

            // Load user credentials and role for authentication
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginRequest.getEmail());
            System.out.println("Loaded user details for JWT generation");

            List<String> authorities = userDetails.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .collect(Collectors.toList());

            String role = "USER";
            Long userId = null;

            for (String authority : authorities) {
                if (authority.startsWith("ROLE_")) {
                    role = authority.substring(5); // Remove "ROLE_"
                } else if (authority.startsWith("ID_")) {
                    userId = Long.parseLong(authority.substring(3)); // Remove "ID_"
                }
            }

            System.out.println("Role: " + role);
            System.out.println("User ID: " + userId);
//            String role = userDetails.getAuthorities().stream()
//                    .map(authority -> authority.getAuthority())
//                    .findFirst()
//                    .orElse("USER");
//
//            if (role.startsWith("ROLE_")) {
//                role = role.substring(5);
//            }
            // Generate token
            String token = jwtUtil.generateToken(userDetails);
            System.out.println("JWT generated: " + token);

            return ResponseEntity.ok(new AuthResponse(token, role, userId));
        } catch (Exception e) {
            System.out.println("Authentication failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

}
