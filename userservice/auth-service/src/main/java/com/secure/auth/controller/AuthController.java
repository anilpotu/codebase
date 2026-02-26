package com.secure.auth.controller;

import com.secure.auth.dto.LoginRequest;
import com.secure.auth.dto.LoginResponse;
import com.secure.auth.dto.RefreshTokenRequest;
import com.secure.auth.dto.RegisterRequest;
import com.secure.auth.dto.TokenResponse;
import com.secure.auth.entity.User;
import com.secure.auth.service.AuthService;
import com.secure.auth.service.TokenService;
import com.secure.common.dto.ApiResponse;
import com.secure.common.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register - username={}", request.getUsername());
        try {
            User user = authService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<User>builder()
                    .success(true)
                    .message("User registered successfully")
                    .data(user)
                    .build());
        } catch (Exception e) {
            log.warn("Registration failed for username={}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<User>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login - username={}", request.getUsername());
        try {
            LoginResponse response = authService.authenticate(request);
            return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .success(true)
                .message("Login successful")
                .data(response)
                .build());
        } catch (Exception e) {
            log.warn("Login failed for username={}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<LoginResponse>builder()
                    .success(false)
                    .message("Invalid credentials")
                    .build());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /auth/refresh");
        try {
            TokenResponse response = tokenService.refreshAccessToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.<TokenResponse>builder()
                .success(true)
                .message("Token refreshed successfully")
                .data(response)
                .build());
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<TokenResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        log.info("POST /auth/logout");
        try {
            String username = authentication.getName();
            authService.logout(username);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Logout successful")
                .build());
        } catch (Exception e) {
            log.warn("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validate(@RequestHeader("Authorization") String authHeader) {
        log.debug("GET /auth/validate");
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                boolean isValid = jwtTokenProvider.validateToken(token);
                return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .success(true)
                    .message("Token is valid")
                    .data(isValid)
                    .build());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Boolean>builder()
                    .success(false)
                    .message("Invalid authorization header")
                    .data(false)
                    .build());
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Boolean>builder()
                    .success(false)
                    .message("Token validation failed")
                    .data(false)
                    .build());
        }
    }
}
