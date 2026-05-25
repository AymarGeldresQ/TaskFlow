package dev.taskflow.integration;

import dev.taskflow.application.dto.auth.AuthResponse;
import dev.taskflow.application.dto.auth.LoginRequest;
import dev.taskflow.application.dto.auth.RefreshRequest;
import dev.taskflow.application.dto.auth.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Auth API Integration Tests")
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/auth";

    @Nested
    @DisplayName("POST /register")
    class Register {

        @Test
        void registersNewUserAndReturnsTokens() {
            RegisterRequest request = new RegisterRequest(
                "john@example.com", "SecurePass123!", "John Doe"
            );

            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE + "/register", request, AuthResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
            assertThat(response.getBody().refreshToken()).isNotBlank();
            assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
            assertThat(response.getBody().user().email()).isEqualTo("john@example.com");
        }

        @Test
        void returns409WhenEmailAlreadyTaken() {
            RegisterRequest request = new RegisterRequest(
                "duplicate@example.com", "SecurePass123!", "Jane Doe"
            );
            restTemplate.postForEntity(BASE + "/register", request, Object.class);

            ResponseEntity<Object> second = restTemplate.postForEntity(
                BASE + "/register", request, Object.class
            );

            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void returns400WhenEmailInvalid() {
            RegisterRequest request = new RegisterRequest("not-an-email", "SecurePass123!", "John Doe");

            ResponseEntity<Object> response = restTemplate.postForEntity(
                BASE + "/register", request, Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns400WhenPasswordTooShort() {
            RegisterRequest request = new RegisterRequest("user@example.com", "short", "John Doe");

            ResponseEntity<Object> response = restTemplate.postForEntity(
                BASE + "/register", request, Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /login")
    class Login {

        @Test
        void loginWithValidCredentials() {
            RegisterRequest reg = new RegisterRequest("login@example.com", "SecurePass123!", "Login User");
            restTemplate.postForEntity(BASE + "/register", reg, AuthResponse.class);

            LoginRequest login = new LoginRequest("login@example.com", "SecurePass123!");
            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE + "/login", login, AuthResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
        }

        @Test
        void returns401WithWrongPassword() {
            RegisterRequest reg = new RegisterRequest("badpass@example.com", "CorrectPass123!", "User");
            restTemplate.postForEntity(BASE + "/register", reg, Object.class);

            LoginRequest login = new LoginRequest("badpass@example.com", "WrongPassword!");
            ResponseEntity<Object> response = restTemplate.postForEntity(
                BASE + "/login", login, Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void returns401WithUnknownEmail() {
            LoginRequest login = new LoginRequest("nobody@example.com", "AnyPassword123!");
            ResponseEntity<Object> response = restTemplate.postForEntity(
                BASE + "/login", login, Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("POST /refresh")
    class Refresh {

        @Test
        void rotatesRefreshTokenAndIssuesNewAccessToken() {
            RegisterRequest reg = new RegisterRequest("refresh@example.com", "SecurePass123!", "Refresh User");
            AuthResponse initial = restTemplate.postForEntity(
                BASE + "/register", reg, AuthResponse.class
            ).getBody();

            assertThat(initial).isNotNull();

            RefreshRequest refreshRequest = new RefreshRequest(initial.refreshToken());
            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE + "/refresh", refreshRequest, AuthResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
            assertThat(response.getBody().refreshToken()).isNotEqualTo(initial.refreshToken());
        }

        @Test
        void returns401WithInvalidRefreshToken() {
            RefreshRequest request = new RefreshRequest("completely-invalid-token");
            ResponseEntity<Object> response = restTemplate.postForEntity(
                BASE + "/refresh", request, Object.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void returns401WhenRefreshTokenUsedTwice() {
            RegisterRequest reg = new RegisterRequest("reuse@example.com", "SecurePass123!", "Reuse User");
            AuthResponse initial = restTemplate.postForEntity(
                BASE + "/register", reg, AuthResponse.class
            ).getBody();

            assertThat(initial).isNotNull();

            RefreshRequest refreshRequest = new RefreshRequest(initial.refreshToken());
            restTemplate.postForEntity(BASE + "/refresh", refreshRequest, AuthResponse.class);

            // Second use of same token should fail (rotated)
            ResponseEntity<Object> second = restTemplate.postForEntity(
                BASE + "/refresh", refreshRequest, Object.class
            );

            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("POST /logout")
    class Logout {

        @Test
        void logoutRevokesAllTokensForUser() {
            RegisterRequest reg = new RegisterRequest("logout@example.com", "SecurePass123!", "Logout User");
            AuthResponse tokens = restTemplate.postForEntity(
                BASE + "/register", reg, AuthResponse.class
            ).getBody();

            assertThat(tokens).isNotNull();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tokens.accessToken());
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                BASE + "/logout", HttpMethod.POST, request, Void.class
            );

            assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // Refresh token should now be invalid
            RefreshRequest refreshRequest = new RefreshRequest(tokens.refreshToken());
            ResponseEntity<Object> refreshResponse = restTemplate.postForEntity(
                BASE + "/refresh", refreshRequest, Object.class
            );

            assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
