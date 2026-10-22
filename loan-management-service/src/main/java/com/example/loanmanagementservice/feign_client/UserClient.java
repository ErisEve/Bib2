package com.example.loanmanagementservice.feign_client;

import com.example.loanmanagementservice.config.FeignConfig;
import com.example.loanmanagementservice.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name = "gateway-service", url = "${GATEWAY_URL:http://localhost:8080}", contextId = "userClient", configuration = FeignConfig.class)
public interface UserClient {
    @GetMapping("/api/users/{id}")
    User getUser(@PathVariable Long id);

    @GetMapping("api/users/findByEmail/{email}")
    Optional<User> findUserByEmail(@PathVariable String email);

    @GetMapping("existsByEmail/{email}")
    boolean existsByEmail(@PathVariable String email);
}
