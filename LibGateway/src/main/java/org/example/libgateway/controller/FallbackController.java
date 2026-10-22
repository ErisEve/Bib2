package org.example.libgateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
// The CircuitBreaker filter forwards the *original* request (method included) here, so these
// handlers must accept every HTTP method or a timed-out POST would surface as a 405.
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/users")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "User service is temporarily unavailable. Please try again later.");
        response.put("service", "user-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @RequestMapping("/books")
    public ResponseEntity<Map<String, Object>> libraryServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Library service is temporarily unavailable. Please try again later.");
        response.put("service", "library-management-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @RequestMapping("/lendings")
    public ResponseEntity<Map<String, Object>> loanServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Loan service is temporarily unavailable. Please try again later.");
        response.put("service", "loan-management-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @RequestMapping("/seats")
    public ResponseEntity<Map<String, Object>> seatServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Seat service is temporarily unavailable. Please try again later.");
        response.put("service", "seat-management-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }


}