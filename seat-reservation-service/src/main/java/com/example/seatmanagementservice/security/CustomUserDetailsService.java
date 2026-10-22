package com.example.seatmanagementservice.security;

import com.example.seatmanagementservice.feign_client.UserClient;
import com.example.seatmanagementservice.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    public final UserClient userClient;

    @Autowired
    public CustomUserDetailsService(UserClient userClient){
        this.userClient = userClient;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("Attempting login for email: " + email);

        User user = userClient.findUserByEmail(email)
                .orElseThrow(() -> {
                    System.out.println("User not found with email: " + email);
                    return new UsernameNotFoundException("User not found with email: "
                            + email);
                });
        List<GrantedAuthority> authorities = List.of(
                // role stored as USER or ADMIN
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

        System.out.println("User found: " + user.getEmail());
        System.out.println("Password hash in DB: " + user.getPassword());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                // use authorities instead of roles not to get ROLE_ROLE_USER
//                .authorities(user.getRole())
                .authorities(authorities)
                .build();
    }
}
