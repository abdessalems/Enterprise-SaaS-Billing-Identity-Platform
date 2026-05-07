package com.saas.security;

import com.saas.entity.Role;
import com.saas.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private static final String SECRET =
        "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 86_400_000L;

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "expiration", EXPIRATION);
    }

    private User buildUser() {
        return User.builder()
            .id(UUID.randomUUID())
            .firstName("Abdessalem")
            .lastName("Saadaoui")
            .email("test@example.com")
            .password("encoded-password")
            .role(Role.USER)
            .build();
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        String token = jwtUtils.generateToken(buildUser());
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsUserEmail() {
        User user = buildUser();
        String token = jwtUtils.generateToken(user);
        assertThat(jwtUtils.extractUsername(token)).isEqualTo("test@example.com");
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        User user = buildUser();
        String token = jwtUtils.generateToken(user);
        assertThat(jwtUtils.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForDifferentUser() {
        User user = buildUser();
        User other = User.builder()
            .id(UUID.randomUUID())
            .email("other@example.com")
            .password("pass")
            .role(Role.USER)
            .build();

        String token = jwtUtils.generateToken(user);
        assertThat(jwtUtils.isTokenValid(token, other)).isFalse();
    }

    @Test
    void isTokenValid_throwsExpiredJwtException_forExpiredToken() {
        JwtUtils expiredJwt = new JwtUtils();
        ReflectionTestUtils.setField(expiredJwt, "secretKey", SECRET);
        ReflectionTestUtils.setField(expiredJwt, "expiration", -1000L);

        User user = buildUser();
        String token = expiredJwt.generateToken(user);

        assertThatThrownBy(() -> expiredJwt.isTokenValid(token, user))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void generateToken_containsUserIdClaim() {
        User user = buildUser();
        String token = jwtUtils.generateToken(user);
        String userId = jwtUtils.extractClaim(token, claims -> claims.get("userId", String.class));
        assertThat(userId).isEqualTo(user.getId().toString());
    }

    @Test
    void generateToken_containsRoleClaim() {
        User user = buildUser();
        String token = jwtUtils.generateToken(user);
        String role = jwtUtils.extractClaim(token, claims -> claims.get("role", String.class));
        assertThat(role).isEqualTo("USER");
    }

    @Test
    void extractUsername_throwsForTamperedToken() {
        User user = buildUser();
        String token = jwtUtils.generateToken(user) + "tampered";
        assertThatThrownBy(() -> jwtUtils.extractUsername(token))
            .isInstanceOf(JwtException.class);
    }
}
