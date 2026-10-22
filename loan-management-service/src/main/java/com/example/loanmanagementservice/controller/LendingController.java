package com.example.loanmanagementservice.controller;

import com.example.loanmanagementservice.dto.LoanRequest;
import com.example.loanmanagementservice.feign_client.BookClient;
import com.example.loanmanagementservice.feign_client.UserClient;
import com.example.loanmanagementservice.model.Book;
import com.example.loanmanagementservice.model.Lending;
import com.example.loanmanagementservice.model.User;
import com.example.loanmanagementservice.service.LendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lendings")
public class LendingController {
    @Autowired
    private UserClient userClient;
    @Autowired
    private BookClient bookClient;
    @Autowired
    private LendingService lendingService;

    @Operation(summary = "Create a new lending record")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/loanABook")
    public ResponseEntity<Lending> saveLending(@RequestBody LoanRequest request) {

        Optional<User> user = userClient.findUserByEmail(request.getUserEmail());
        if(user.isEmpty()){
            throw new Error("User does not exist");
        }

        if (request.getBookId() == null) {
//            ResponseEntity.badRequest().body("Book ID is required");
//            return ResponseEntity.badRequest(null);

        }

        Optional<Book> book = bookClient.getBookById(request.getBookId());
        if(book.isEmpty()){
            throw new Error("Book does not exist");
        }

        Lending lending = new Lending(user.get(),book.get(), LocalDate.now(), LocalDate.now().plusDays(request.getDays()));
        Lending saved = lendingService.saveLending(lending);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Get all the lending records")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<Lending>> getAllLendings() {
        return ResponseEntity.ok(lendingService.getAllLendings());
    }

    @Operation(summary = "Get lending records by user email")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/user-email")
    public ResponseEntity<List<Lending>> getLendingsByUserEmail(@RequestParam String email) {
        return ResponseEntity.ok(lendingService.getLendingsByUserEmail(email));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteLoan(@PathVariable Long id) {
        if (lendingService.existsById(id)) {
            lendingService.deleteById(id);
            return ResponseEntity.ok("Loan with ID " + id + " deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Loan with ID " + id + " not found.");
        }
    }
}
