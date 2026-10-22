package com.example.librarymanagementservice.service;

import com.example.librarymanagementservice.model.Book;
import com.example.librarymanagementservice.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book save(Book book) {
        return bookRepository.save(book);
    }

    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id)
                .map(book -> withAvailability(book, bookRepository.isBookLoaned(id)));
    }

    public List<Book> findAll() {
        return withAvailability(bookRepository.findAll());
    }

    public List<Book> findAllAvailable() {
        List<Book> books = bookRepository.findAllAvailable();
        books.forEach(book -> withAvailability(book, false));
        return books;
    }

    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return bookRepository.existsById(id);
    }

    public List<Book> searchByKeyword(String keyword) {
        return withAvailability(bookRepository.searchByKeyword(keyword.toLowerCase()));
    }

    /**
     * Availability is owned by the lending records, not by the books table: the
     * stored "available" column was never maintained, so it is recomputed here
     * for every book leaving this service.
     */
    private List<Book> withAvailability(List<Book> books) {
        Set<Long> loanedIds = new HashSet<>(bookRepository.findLoanedBookIds());
        books.forEach(book -> withAvailability(book, loanedIds.contains(book.getId())));
        return books;
    }

    private Book withAvailability(Book book, boolean loaned) {
        book.setAvailable(!loaned);
        return book;
    }

    public List<Map<String, Object>> getTopActiveUsers() {
        List<Object[]> results = bookRepository.findTopActiveUsers();

        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userEmail", row[0]);
                    map.put("booksFetched", row[1]);
                    return map;
                })
                .collect(Collectors.toList());
    }

}
