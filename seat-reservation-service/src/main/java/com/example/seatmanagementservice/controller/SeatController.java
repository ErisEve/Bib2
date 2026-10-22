package com.example.seatmanagementservice.controller;

import com.example.seatmanagementservice.dto.SeatReservationDTO;
import com.example.seatmanagementservice.feign_client.UserClient;
import com.example.seatmanagementservice.model.Seat;
import com.example.seatmanagementservice.model.User;
import com.example.seatmanagementservice.service.SeatService;
import com.netflix.discovery.converters.Auto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Seats", description = "Operations related to seats")
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/seats")
public class SeatController {
    @Autowired
    UserClient userClient;

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @Operation(summary = "Get all seats",
            description = "Returns a list of seats")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of seats retrieved successfully")
    })
    @GetMapping("")
    public List<Seat> getAllSeats() {
        List<Seat> seats = seatService.findAll();
        Collections.reverse(seats);
        return seats;
    }

    @PutMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveSeatByPath(
            @Valid @RequestBody SeatReservationDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth.getName();

        User currentUser = userClient.findUserByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        System.out.println("Im in the controller. request: "+request.toString());
        if (request.getEmail() != null && request.getSeatId() != null && request.getUserId() != null) {
            Seat s = seatService.reserveSeat(request.getSeatId(), request.getUserId());
            System.out.println("I put it together: "+s) ;
            response.put("message", "Seat updated successfully");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(response);
    }
}

