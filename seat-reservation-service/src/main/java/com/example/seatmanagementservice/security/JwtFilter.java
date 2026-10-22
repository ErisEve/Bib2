package com.example.seatmanagementservice.security;

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

        // Skip JWT validation for public endpoints
        if (path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;

        System.out.println("Seat Service - Processing request: " + path);
        System.out.println("Seat Service - Authorization header: " + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);
            try {
                username = jwtUtil.extractEmail(jwtToken);
                System.out.println("Seat Service - Extracted username: " + username);
                System.out.println("Seat Service - Extracted roles: " + jwtUtil.extractRoles(jwtToken));
            } catch (Exception e) {
                System.out.println("Seat Service - Invalid JWT Token: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwtToken, userDetails)) {
                System.out.println("Seat Service - JWT validated for user: " + username);

                List<String> roles = jwtUtil.extractRoles(jwtToken);
                System.out.println("Seat Service - Roles extracted: " + roles);

                List<String> properRoles = roles.stream()
                        .filter(role -> !role.startsWith("ID_"))
                        .collect(Collectors.toList());

                // If no proper role found, use the first role
                if (properRoles.isEmpty() && !roles.isEmpty()) {
                    properRoles = List.of(roles.get(0));
                }

                List<GrantedAuthority> authorities = properRoles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                System.out.println("Seat Service - Authorities: " + authorities);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("Seat Service - JWT validation failed for user: " + username);
            }
        }

        filterChain.doFilter(request, response);
    }
}