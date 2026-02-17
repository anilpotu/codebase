package com.secure.order.service;

import com.secure.common.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users/{userId}")
    UserDTO getUserById(
            @PathVariable("userId") Long userId,
            @RequestHeader("Authorization") String token
    );

}
