package org.example.bibliotekaui.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    private String email;
    private String password;
    private String role;  // Optional, for registration

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}