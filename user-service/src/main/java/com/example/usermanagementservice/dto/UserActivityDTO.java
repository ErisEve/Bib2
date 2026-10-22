package com.example.usermanagementservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User activity data with role and book count")
public class UserActivityDTO {

    @Schema(description = "User's email", example = "user@example.com")
    private String email;

    @Schema(description = "User's role", example = "ROLE_USER")
    private String role;

    @Schema(description = "Number of books fetched by the user", example = "5")
    private Long bookCount;

    public UserActivityDTO(){

    }

    public UserActivityDTO(String email, String role, Long bookCount) {
        this.email = email;
        this.role = role;
        this.bookCount = bookCount;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getBookCount() {
        return bookCount;
    }

    public void setBookCount(Long bookCount) {
        this.bookCount = bookCount;
    }

    @Override
    public String toString() {
        return "UserActivityDTO{" +
                "email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", bookCount=" + bookCount +
                '}';
    }
}


