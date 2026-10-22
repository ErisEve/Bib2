package com.example.seatmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SeatManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeatManagerApplication.class, args);
    }

}
