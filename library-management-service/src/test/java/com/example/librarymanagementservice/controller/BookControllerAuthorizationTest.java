package com.example.librarymanagementservice.controller;

import com.example.librarymanagementservice.model.Book;
import com.example.librarymanagementservice.security.CustomUserDetailsService;
import com.example.librarymanagementservice.security.JwtFilter;
import com.example.librarymanagementservice.security.JwtUtil;
import com.example.librarymanagementservice.security.SecurityConfig;
import com.example.librarymanagementservice.service.BookService;
import com.example.librarymanagementservice.service.ExternalBookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class BookControllerAuthorizationTest {

    private static final String BOOK_JSON = "{\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"isbn\":\"9780441172719\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean private BookService bookService;
    @MockBean private ExternalBookService externalBookService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private void stubSaves() {
        Book saved = new Book();
        saved.setId(1L);
        when(bookService.save(any(Book.class))).thenReturn(saved);
        when(externalBookService.fetchFromOpenLibrary(anyString())).thenReturn(new Book());
    }

    @Test
    @WithMockUser(roles = "USER")
    void readersCannotCreateBooks() throws Exception {
        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/books/add").contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/books/fetch").param("isbn", "9780441172719"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void librariansCanCreateBooks() throws Exception {
        stubSaves();
        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/books/add").contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/books/fetch").param("isbn", "9780441172719"))
                .andExpect(status().isOk());
    }
}
