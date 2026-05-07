package com.saas.integration;

import com.saas.dto.auth.AuthResponse;
import com.saas.dto.auth.LoginRequest;
import com.saas.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    private RegisterRequest registerRequest(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Abdessalem");
        req.setLastName("Saadaoui");
        req.setEmail(email);
        req.setPassword("password123");
        return req;
    }

    @Test
    void register_returnsCreatedWithJwt() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/register", registerRequest("register1@test.com"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getUser().getEmail()).isEqualTo("register1@test.com");
    }

    @Test
    void register_returnsConflict_whenEmailAlreadyTaken() {
        restTemplate.postForEntity("/api/v1/auth/register",
            registerRequest("duplicate@test.com"), Object.class);

        ResponseEntity<Object> response = restTemplate.postForEntity("/api/v1/auth/register",
            registerRequest("duplicate@test.com"), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_returnsBadRequest_whenPasswordTooShort() {
        RegisterRequest req = registerRequest("short@test.com");
        req.setPassword("123");

        ResponseEntity<Object> response = restTemplate.postForEntity(
            "/api/v1/auth/register", req, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_returnsOkWithJwt_forValidCredentials() {
        restTemplate.postForEntity("/api/v1/auth/register",
            registerRequest("login@test.com"), Object.class);

        LoginRequest login = new LoginRequest();
        login.setEmail("login@test.com");
        login.setPassword("password123");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login", login, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAccessToken()).isNotBlank();
    }

    @Test
    void login_returnsUnauthorized_forWrongPassword() {
        restTemplate.postForEntity("/api/v1/auth/register",
            registerRequest("wrongpass@test.com"), Object.class);

        LoginRequest login = new LoginRequest();
        login.setEmail("wrongpass@test.com");
        login.setPassword("wrongpassword");

        ResponseEntity<Object> response = restTemplate.postForEntity(
            "/api/v1/auth/login", login, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void register_automaticallyCreatesFreePlanSubscription() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/register", registerRequest("freesub@test.com"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUser().getRole()).isEqualTo("USER");
    }
}
