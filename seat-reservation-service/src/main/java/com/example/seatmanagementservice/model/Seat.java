package com.example.seatmanagementservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_number", nullable = false, unique = true)
    private String seat_number;

    @OneToOne
    @JoinColumn(name = "reserved_by", referencedColumnName = "id")
    private User user;

    public Long getReservedBy() {
        return user != null ? user.getId() : null;
    }

    public String getSeatNumber() {
        return seat_number;
    }
}
