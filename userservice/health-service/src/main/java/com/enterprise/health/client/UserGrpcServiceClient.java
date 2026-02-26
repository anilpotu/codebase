package com.enterprise.health.client;

import com.enterprise.health.dto.GrpcUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for user-grpc-service.
 * Used to verify that a user exists before creating a health record.
 */
@FeignClient(name = "user-grpc-service")
public interface UserGrpcServiceClient {

    @GetMapping("/api/users/{userId}")
    ResponseEntity<GrpcUserDTO> getUserById(@PathVariable("userId") Long userId);
}
