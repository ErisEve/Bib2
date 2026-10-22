package com.example.loanmanagementservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "books",
        indexes = {
                @Index(name = "idx_fetched_by", columnList = "fetched_by")
        }
        )
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(unique = true)
    private String isbn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "fetched_by")
    private String fetchedBy;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "available")
    @Builder.Default
    private Boolean available = true;

}
