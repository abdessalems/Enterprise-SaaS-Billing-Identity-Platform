package com.saas.service;

import com.saas.dto.auth.AuthResponse;
import com.saas.dto.auth.LoginRequest;
import com.saas.dto.auth.RegisterRequest;
import com.saas.entity.Role;
import com.saas.entity.User;
import com.saas.exception.EmailAlreadyExistsException;
import com.saas.repository.UserRepository;
import com.saas.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private SubscriptionService subscriptionService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86_400_000L);
    }

    private RegisterRequest buildRegisterRequest(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Abdessalem");
        req.setLastName("Saadaoui");
        req.setEmail(email);
        req.setPassword("password123");
        return req;
    }

    private User buildUser(String email) {
        return User.builder()
            .id(UUID.randomUUID())
            .firstName("Abdessalem")
            .lastName("Saadaoui")
            .email(email)
            .password("encoded")
            .role(Role.USER)
            .build();
    }

    @Test
    void register_success_returnsAuthResponseWithToken() {
        RegisterRequest req = buildRegisterRequest("new@example.com");

        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("bcrypt-hash");
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(jwtUtils.generateToken(any())).willReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void register_createsUserWithEncodedPassword() {
        RegisterRequest req = buildRegisterRequest("enc@example.com");

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("bcrypt-hash");
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getPassword()).isEqualTo("bcrypt-hash");
            assertThat(u.getRole()).isEqualTo(Role.USER);
            return u;
        });
        given(jwtUtils.generateToken(any())).willReturn("token");

        authService.register(req);

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_createsFreeSubscriptionForNewUser() {
        RegisterRequest req = buildRegisterRequest("sub@example.com");

        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("hash");
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(jwtUtils.generateToken(any())).willReturn("token");

        authService.register(req);

        verify(subscriptionService).createFreeSubscription(any(User.class));
    }

    @Test
    void register_throwsEmailAlreadyExists_whenEmailTaken() {
        RegisterRequest req = buildRegisterRequest("taken@example.com");
        given(userRepository.existsByEmail("taken@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessageContaining("taken@example.com");

        verifyNoInteractions(passwordEncoder, jwtUtils, subscriptionService);
    }

    @Test
    void login_success_returnsAuthResponseWithToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("password123");

        User user = buildUser("user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(jwtUtils.generateToken(user)).willReturn("jwt-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void login_delegatesCredentialCheckToAuthenticationManager() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("password123");

        User user = buildUser("user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(jwtUtils.generateToken(any())).willReturn("token");

        authService.login(req);

        verify(authenticationManager).authenticate(
            new UsernamePasswordAuthenticationToken("user@example.com", "password123")
        );
    }
}
