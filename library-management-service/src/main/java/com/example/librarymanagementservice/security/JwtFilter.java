package com.example.librarymanagementservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        System.out.println("========================================");
        System.out.println("JWT Filter Processing: " + path);
        System.out.println("Method: " + request.getMethod());

        // Skip JWT validation for public endpoints
        if (path.startsWith("/api/auth")) {
            System.out.println("Skipping auth for public endpoint: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization Header: " + (authHeader != null ? "PRESENT" : "MISSING"));

        String username = null;
        String jwtToken = null;

        // Check Authorization header
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            // Remove "Bearer "
            jwtToken = authHeader.substring(7);
            System.out.println("Token extracted (first 30 chars): " + jwtToken.substring(0, Math.min(30, jwtToken.length())) + "...");
            System.out.println("Token length: " + jwtToken.length());

            try {
                // get username (subject) from token
                username = jwtUtil.extractUsername(jwtToken);
                System.out.println("Username extracted: " + username);
            } catch (Exception e) {
                System.err.println("Invalid JWT Token: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No valid Authorization header found");
            // For testing, continue without authentication
            filterChain.doFilter(request, response);
            return;
        }

        // If token is valid and user is not yet authenticated - Loads user from DB using username
        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("Loading user details for: " + username);

            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                System.out.println("User details loaded: " + userDetails.getUsername());
                System.out.println("User authorities: " + userDetails.getAuthorities());

                // Validates the JWT against that user
                if(jwtUtil.validateToken(jwtToken, userDetails)) {
                    System.out.println("JWT validated for user: " + username);

                    // Extract roles from token and convert them to GrantedAuthority
                    List<String> roles = jwtUtil.extractRoles(jwtToken);
                    System.out.println("Roles from token: " + roles);

                    List<GrantedAuthority> authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    System.out.println("Authorities: " + authorities);

                    // Creates a Spring Security Authentication token
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, jwtToken, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Stores it in the security context, marking the user as authenticated
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("Authentication set in SecurityContext");
                } else {
                    System.err.println("JWT validation failed for user: " + username);
                }
            } catch (Exception e) {
                System.err.println("Error loading user or validating token: " + e.getMessage());
                e.printStackTrace();
                // Don't set authentication
            }
        } else {
            if (username == null) {
                System.out.println("Username is null - token extraction failed");
            }
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                System.out.println("Authentication already exists in context");
            }
        }

        System.out.println("========================================");
        // Pass the request to the next filter
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        // Skip filter for these paths
        return path.startsWith("/api/auth") ||
                path.startsWith("/actuator") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }
}