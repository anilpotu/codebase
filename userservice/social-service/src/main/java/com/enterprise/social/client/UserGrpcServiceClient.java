package com.enterprise.social.client;

import com.enterprise.social.dto.GrpcUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for user-grpc-service.
 * Used to verify that users exist before social operations are persisted.
 */
@FeignClient(name = "user-grpc-service")
public interface UserGrpcServiceClient {

    @GetMapping("/api/users/{userId}")
    ResponseEntity<GrpcUserDTO> getUserById(@PathVariable("userId") Long userId);
}
