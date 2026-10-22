package org.example.libgateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Boots the whole service on a random port and checks the health endpoint the Kubernetes probes
// rely on. Needs no running infrastructure.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LibGatewayApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GatewayProperties gatewayProperties;

    @Test
    void startsAndReportsHealthy() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    // A fallbackUri in application.yml that FallbackController does not map falls through to the
    // catch-all /** route and is proxied to ui-service instead (the loan route once pointed at
    // /fallback/loans). That proxied request also fails, so the body is what tells the two apart.
    @Test
    void everyCircuitBreakerFallbackIsServed() {
        List<String> fallbacks = gatewayProperties.getRoutes().stream()
                .flatMap(route -> route.getFilters().stream())
                .filter(filter -> filter.getName().equals("CircuitBreaker"))
                .map(FilterDefinition::getArgs)
                .map(args -> args.get("fallbackUri"))
                .toList();

        assertThat(fallbacks).hasSize(4);
        for (String fallback : fallbacks) {
            assertThat(fallback).startsWith("forward:");
            ResponseEntity<Map> response = restTemplate.getForEntity(fallback.substring("forward:".length()), Map.class);
            assertThat(response.getStatusCode()).as(fallback).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).as(fallback)
                    .containsEntry("status", "error")
                    .containsKey("service");
        }
    }
}
