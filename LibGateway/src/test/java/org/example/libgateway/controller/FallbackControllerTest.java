package org.example.libgateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FallbackController.class)
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    // The gateway forwards to the fallback with the original request method intact, so a timed-out
    // POST /api/books/fetch arrives here as POST /fallback/books. Every method must get the 503.
    @Test
    void fallbackAnswers503ForEveryMethod() {
        for (String path : new String[] {"/users", "/books", "/lendings", "/seats"}) {
            for (HttpMethod method : new HttpMethod[] {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}) {
                webTestClient.method(method).uri("/fallback" + path).exchange()
                        .expectStatus().isEqualTo(503)
                        .expectBody().jsonPath("$.status").isEqualTo("error")
                        .jsonPath("$.message").isNotEmpty();
            }
        }
    }
}
