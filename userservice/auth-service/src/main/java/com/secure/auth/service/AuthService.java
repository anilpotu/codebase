package com.secure.auth.service;

import com.secure.auth.dto.LoginRequest;
import com.secure.auth.dto.LoginResponse;
import com.secure.auth.dto.RegisterRequest;
import com.secure.auth.entity.RefreshToken;
import com.secure.auth.entity.Role;
import com.secure.auth.entity.User;
import com.secure.auth.repository.RefreshTokenRepository;
import com.secure.auth.repository.RoleRepository;
import com.secure.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Transactional
    public User registerUser(RegisterRequest request) {
        log.info("Registering user: {}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("Default role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .enabled(true)
            .accountNonLocked(true)
            .createdAt(LocalDateTime.now())
            .roles(roles)
            .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", request.getUsername());
        return savedUser;
    }

    @Transactional
    public LoginResponse authenticate(LoginRequest request) {
        log.info("Authenticating user: {}", request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = tokenService.generateAccessToken(user);
        RefreshToken refreshToken = tokenService.generateRefreshToken(user);

        log.info("User authenticated: {}", request.getUsername());
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken.getToken())
            .tokenType("Bearer")
            .expiresIn(3600000L)
            .build();
    }

    @Transactional
    public void logout(String username) {
        log.info("Logging out user: {}", username);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        tokenService.revokeUserTokens(user);
    }
}
