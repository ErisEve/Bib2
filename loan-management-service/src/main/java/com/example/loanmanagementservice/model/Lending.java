package com.example.loanmanagementservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "lending", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "book_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lending {

    public Lending(User user, Book book, LocalDate borrowDate, LocalDate returnDate){
        this.book = book;
        this.user = user;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Book book;

    private LocalDate borrowDate;
    private LocalDate returnDate;
}
