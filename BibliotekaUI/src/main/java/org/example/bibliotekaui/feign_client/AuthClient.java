package org.example.bibliotekaui.feign_client;

import org.example.bibliotekaui.config.FeignConfig;
import org.example.bibliotekaui.dto.AuthRequest;
import org.example.bibliotekaui.dto.AuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "gateway-service",
        url = "${GATEWAY_URL:http://localhost:8080}",
        contextId = "authrClient"
)
public interface AuthClient {
    @PostMapping("/api/auth/login")
    AuthResponse login(@RequestBody AuthRequest authRequest);

    @PostMapping("/api/auth/register")
    AuthResponse register(@RequestBody AuthRequest authRequest);
}

