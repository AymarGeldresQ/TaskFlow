package dev.taskflow.integration;

import dev.taskflow.application.dto.auth.AuthResponse;
import dev.taskflow.application.dto.auth.RegisterRequest;
import java.util.UUID;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("taskflow_test")
            .withUsername("taskflow")
            .withPassword("taskflow_test");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @BeforeEach
    void configureHttpClient() {
        restTemplate.getRestTemplate().setRequestFactory(
            new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault())
        );
    }

    protected AuthResponse register(String email, String password, String fullName) {
        RegisterRequest request = new RegisterRequest(email, password, fullName);
        AuthResponse response = restTemplate.postForObject("/api/v1/auth/register", request, AuthResponse.class);
        if (response == null) { throw new IllegalStateException("Registration failed for: " + email); }
        return response;
    }

    protected String registerAndGetToken(String email, String password, String fullName) {
        return register(email, password, fullName).accessToken();
    }

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected <T> HttpEntity<T> authRequest(T body, String token) {
        return new HttpEntity<>(body, authHeaders(token));
    }

    protected HttpEntity<Void> authRequest(String token) {
        return new HttpEntity<>(authHeaders(token));
    }
}
