package com.example.seatmanagementservice.service;

import com.example.seatmanagementservice.feign_client.UserClient;
import com.example.seatmanagementservice.model.Seat;
import com.example.seatmanagementservice.model.User;
import com.example.seatmanagementservice.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final UserClient userClient;

    public SeatService(SeatRepository seatRepository, UserClient userClient) {
        this.seatRepository = seatRepository;
        this.userClient = userClient;
    }

    public List<Seat> findAll() {
        return seatRepository.fetchAllSeats();
    }

    public Seat reserveSeat(Long seatId,Long  userId){
        User user = userClient.getUser(userId);
        Seat seat = seatRepository.getById(seatId);
        if (seat.getUser() != null) {
            throw new IllegalStateException("Seat " + seat.getSeatNumber() + " is already reserved");
        }
        System.out.println("What do we have here: " + seat.toString());
        seat.setUser(user);
        return seatRepository.save(seat);
    }

}
