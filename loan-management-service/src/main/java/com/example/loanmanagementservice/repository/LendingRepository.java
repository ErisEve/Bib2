package com.example.loanmanagementservice.repository;

import com.example.loanmanagementservice.model.Lending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LendingRepository extends JpaRepository<Lending, Long> {

    // For overdue
    List<Lending> findByReturnDateIsNull();

    // For support filtering by user email
    List<Lending> findByUserEmail(String email);
}
