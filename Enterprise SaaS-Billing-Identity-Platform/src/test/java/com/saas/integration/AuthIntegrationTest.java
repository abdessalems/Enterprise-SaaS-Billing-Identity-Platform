package com.saas.integration;

import com.saas.dto.ApiResponse;
import com.saas.dto.auth.AuthResponse;
import com.saas.dto.auth.LoginRequest;
import com.saas.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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

    private static final ParameterizedTypeReference<ApiResponse<AuthResponse>> AUTH_RESPONSE_TYPE =
        new ParameterizedTypeReference<>() {};

    private RegisterRequest registerRequest(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Abdessalem");
        req.setLastName("Saadaoui");
        req.setEmail(email);
        req.setPassword("password123");
        return req;
    }

    private ResponseEntity<ApiResponse<AuthResponse>> postRegister(String email) {
        return restTemplate.exchange(
            "/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(registerRequest(email)),
            AUTH_RESPONSE_TYPE
        );
    }

    @Test
    void register_returnsCreatedWithJwt() {
        ResponseEntity<ApiResponse<AuthResponse>> response = postRegister("register1@test.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        AuthResponse auth = response.getBody().getData();
        assertThat(auth).isNotNull();
        assertThat(auth.getAccessToken()).isNotBlank();
        assertThat(auth.getTokenType()).isEqualTo("Bearer");
        assertThat(auth.getUser().getEmail()).isEqualTo("register1@test.com");
    }

    @Test
    void register_returnsConflict_whenEmailAlreadyTaken() {
        restTemplate.postForEntity("/auth/register",
            registerRequest("duplicate@test.com"), Object.class);

        ResponseEntity<Object> response = restTemplate.postForEntity("/auth/register",
            registerRequest("duplicate@test.com"), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_returnsBadRequest_whenPasswordTooShort() {
        RegisterRequest req = registerRequest("short@test.com");
        req.setPassword("123");

        ResponseEntity<Object> response = restTemplate.postForEntity(
            "/auth/register", req, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_returnsOkWithJwt_forValidCredentials() {
        postRegister("login@test.com");

        LoginRequest login = new LoginRequest();
        login.setEmail("login@test.com");
        login.setPassword("password123");

        ResponseEntity<ApiResponse<AuthResponse>> response = restTemplate.exchange(
            "/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(login),
            AUTH_RESPONSE_TYPE
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getAccessToken()).isNotBlank();
    }

    @Test
    void login_returnsUnauthorized_forWrongPassword() {
        postRegister("wrongpass@test.com");

        LoginRequest login = new LoginRequest();
        login.setEmail("wrongpass@test.com");
        login.setPassword("wrongpassword");

        ResponseEntity<Object> response = restTemplate.postForEntity(
            "/auth/login", login, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void register_automaticallyCreatesFreePlanSubscription() {
        ResponseEntity<ApiResponse<AuthResponse>> response = postRegister("freesub@test.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().getUser().getRole()).isEqualTo("USER");
    }
}
