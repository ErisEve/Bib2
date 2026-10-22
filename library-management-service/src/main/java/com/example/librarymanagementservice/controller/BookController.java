package com.example.librarymanagementservice.controller;

import com.example.librarymanagementservice.model.Book;
import com.example.librarymanagementservice.service.BookService;
import com.example.librarymanagementservice.service.ExternalBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Tag(name = "Books", description = "Operations related to books")
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final ExternalBookService externalBookService;

    public BookController(BookService bookService, ExternalBookService externalBookService) {
        this.bookService = bookService;
        this.externalBookService = externalBookService;
    }

    @Operation(summary = "Get all books",
            description = "Returns a list of books stored in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of books retrieved successfully")
    })
    @GetMapping("/booksAll")
    public List<Book> getAllBooks() {
        List<Book> books = bookService.findAll();
        Collections.reverse(books);
        return books;
    }


    @Operation(summary = "Get all available books",
            description = "Returns a list of books available for lending")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of books retrieved successfully")
    })
    @GetMapping("/booksAvailable")
    public List<Book> getAllAvailableBooks() {
        List<Book> books = bookService.findAllAvailable();
        Collections.reverse(books);
        return books;
    }

    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam String keyword) {
        return bookService.searchByKeyword(keyword);
    }

    @Operation(summary = "Get book by ID",
            description = "Retrieve detailed information of a book by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book found"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return bookService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping
    public Book createBook(@RequestBody Book book) {
        return bookService.save(book);
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Long id) {
        if (bookService.existsById(id)) {
            bookService.deleteById(id);
            return ResponseEntity.ok("Book with ID " + id + " deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Book with ID " + id + " not found.");
        }
    }

    @Operation(summary = "Fetch a book by ISBN", description = "Fetches book data from external API and saves it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book fetched and saved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/fetch")
    public ResponseEntity<String> fetchAndSaveBook(@Parameter(description =
            "ISBN number of the book to fetch and principal representing the authenticated user")
                                                       @RequestParam String isbn, Principal principal) {
        try {
            Book book = externalBookService.fetchFromOpenLibrary(isbn);
            // Set the fetchedBy email from the authenticated user
            book.setFetchedBy(principal.getName());
            Book saved = bookService.save(book);
            return ResponseEntity.ok("Book fetched and saved with ID: " + saved.getId());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch book with ISBN: " + isbn);
        }
    }

    @Operation(
            summary = "Get top active users",
            description = "Returns a list of users sorted by the number of books they fetched, DESC."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Top active users retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to retrieve user activity")
    })
    @GetMapping("/top-users")
    public ResponseEntity<List<Map<String, Object>>> getTopActiveUsers() {
        return ResponseEntity.ok(bookService.getTopActiveUsers());
    }

    @Operation(summary = "Add a new book manually",
            description = "Adds a new book to the database with manually entered details. " +
                    "Only librarians can add books.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book added successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Book.class))),
            @ApiResponse(responseCode = "400", description = "Invalid book data provided"),
            @ApiResponse(responseCode = "403", description = "Access denied - Librarian role required"),
            @ApiResponse(responseCode = "409", description = "Book with this ISBN already exists")
    })
    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping("/add")
    public ResponseEntity<?> addBookManually(
            @Parameter(description = "Book object to be added", required = true)
            @Valid @RequestBody Book book, Principal principal) {

        try {
            // Set the fetchedBy field to the current librarian
            book.setFetchedBy(principal.getName());

            // Check if book with ISBN already exists
//            if (book.getIsbn() != null && !book.getIsbn().isEmpty()) {
//                if (bookService.existsByIsbn(book.getIsbn())) {
//                    return ResponseEntity.status(HttpStatus.CONFLICT)
//                            .body("Book with ISBN " + book.getIsbn() + " already exists");
//                }
//            }

            Book saved = bookService.save(book);
            return ResponseEntity.ok(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid book data: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add book: " + e.getMessage());
        }
    }
}

