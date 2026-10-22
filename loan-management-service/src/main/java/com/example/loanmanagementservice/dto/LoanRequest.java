package com.example.loanmanagementservice.dto;

import lombok.Data;

@Data
public class LoanRequest {
    private String token;
    private Long bookId;
    private String userEmail;
    private Integer days;
}