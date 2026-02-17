package com.secure.auth.service;

import com.secure.auth.dto.TokenResponse;
import com.secure.auth.entity.RefreshToken;
import com.secure.auth.entity.User;
import com.secure.auth.repository.RefreshTokenRepository;
import com.secure.auth.repository.UserRepository;
import com.secure.common.security.JwtTokenProvider;
import com.secure.common.security.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-token.expiration:604800000}")
    private Long refreshTokenExpiration;

    public String generateAccessToken(User user) {
        log.debug("Generating access token for user: {}", user.getUsername());
        SecurityUser securityUser = SecurityUser.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()))
            .build();
        return jwtTokenProvider.generateToken(securityUser);
    }

    @Transactional
    public RefreshToken generateRefreshToken(User user) {
        log.debug("Generating refresh token for user: {}", user.getUsername());
        RefreshToken refreshToken = RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
            .revoked(false)
            .createdAt(LocalDateTime.now())
            .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public TokenResponse refreshAccessToken(String refreshToken) {
        log.debug("Refreshing access token");
        RefreshToken validatedToken = validateRefreshToken(refreshToken);
        User user = validatedToken.getUser();

        String accessToken = generateAccessToken(user);
        Long expiresIn = jwtTokenProvider.getExpirationTime();

        return TokenResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(expiresIn)
            .build();
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        log.info("Revoking refresh token");
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeUserTokens(User user) {
        log.info("Revoking all tokens for user: {}", user.getUsername());
        refreshTokenRepository.deleteByUser(user);
    }
}
