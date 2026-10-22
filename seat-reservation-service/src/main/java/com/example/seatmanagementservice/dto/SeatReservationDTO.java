package com.example.seatmanagementservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservationDTO {
    @Email(message = "Invalid email format")
    private String email;

    private Long seatId;

    private Long userId;
}
