package com.secure.gateway.filter;

import com.secure.common.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication for public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract JWT token from Authorization header
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            logger.warn("Missing Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Invalid Authorization header format for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Validate token
            if (!jwtTokenProvider.validateToken(token)) {
                logger.warn("Invalid JWT token for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Extract claims
            Claims claims = jwtTokenProvider.getClaimsFromToken(token);
            String username = claims.getSubject();
            String userId = claims.get("userId", String.class);
            String roles = claims.get("roles", String.class);

            // Add user info to request headers for downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .header("X-User-Roles", roles)
                .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

            logger.debug("Authenticated user: {} for path: {}", username, path);
            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            logger.error("JWT authentication failed for path: {}", path, e);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               // SDS auth endpoints (login, register, refresh, validate, logout)
               path.startsWith("/api/auth/") ||
               // Product catalog public read; write ops are secured by product-service itself
               path.startsWith("/api/products/") ||
               // gRPC-origin services validate their own JWTs (different issuer)
               path.startsWith("/api/grpc-users/") ||
               path.startsWith("/api/accounts/") ||
               path.startsWith("/api/transactions/") ||
               path.startsWith("/api/health-records/") ||
               path.startsWith("/api/vitals/") ||
               path.startsWith("/api/profiles/") ||
               path.startsWith("/api/posts/") ||
               path.startsWith("/api/connections/") ||
               // OpenAPI / Swagger
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/webjars/");
    }
}
