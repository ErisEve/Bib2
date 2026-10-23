package com.example.librarymanagementservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Boots the whole service on a random port with the `test` profile (H2 instead of PostgreSQL) and checks the
// health endpoint the Kubernetes probes rely on. Needs no running infrastructure.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LibraryManagerApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void startsAndReportsHealthy() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    // The Kubernetes readiness probe must take the pod out of the Service when the database is
    // unreachable; the liveness probe must not, or a database outage would restart every pod.
    @Test
    void readinessChecksTheDatabaseAndLivenessDoesNot() {
        ResponseEntity<Map> readiness = restTemplate.getForEntity("/actuator/health/readiness", Map.class);
        ResponseEntity<Map> liveness = restTemplate.getForEntity("/actuator/health/liveness", Map.class);

        assertThat(readiness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) readiness.getBody().get("components")).containsKeys("db", "readinessState");
        assertThat(liveness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) liveness.getBody().get("components")).containsOnlyKeys("livenessState");
    }
}
