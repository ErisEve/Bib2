package com.example.seatmanagementservice.repository;

import com.example.seatmanagementservice.model.Seat;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

//    @Query(value = """
//    SELECT fetched_by AS userEmail, COUNT(*) AS booksFetched
//    FROM books
//    WHERE fetched_by IS NOT NULL
//    GROUP BY fetched_by
//    ORDER BY booksFetched DESC
//    """, nativeQuery = true)
//    List<Object[]> findTopActiveUsers();

    @Query(value = """
    SELECT *
    FROM seats
    """, nativeQuery = true)
    List<Seat> fetchAllSeats();

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE seats SET reserved_by = :userID WHERE id = :seatID
                        """, nativeQuery = true)
    int reserveSeat(@Param("userID") Long userID,
                     @Param("seatID") Long seatID);

}
