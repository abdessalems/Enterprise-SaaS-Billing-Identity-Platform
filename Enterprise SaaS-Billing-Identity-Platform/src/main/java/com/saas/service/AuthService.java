package com.saas.service;

import com.saas.dto.auth.AuthResponse;
import com.saas.dto.auth.LoginRequest;
import com.saas.dto.auth.RegisterRequest;
import com.saas.dto.auth.UserResponse;
import com.saas.entity.Role;
import com.saas.entity.User;
import com.saas.exception.EmailAlreadyExistsException;
import com.saas.repository.UserRepository;
import com.saas.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.USER)
            .build();

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        return buildAuthResponse(user);
    }

    // ─── private helpers ─────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
            .accessToken(jwtUtils.generateToken(user))
            .expiresIn(jwtExpiration)
            .user(toUserResponse(user))
            .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .role(user.getRole().name())
            .active(user.isEnabled())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
