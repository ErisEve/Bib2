package com.example.loanmanagementservice.feign_client;

import com.example.loanmanagementservice.config.FeignConfig;
import com.example.loanmanagementservice.model.Book;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name = "gateway-service", url = "${GATEWAY_URL:http://localhost:8080}", contextId = "bookClient", configuration = FeignConfig.class)
public interface BookClient {
    @GetMapping("/api/books/{id}")
    Optional<Book> getBookById(@PathVariable Long id);
}
