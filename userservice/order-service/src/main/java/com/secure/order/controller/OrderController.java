package com.secure.order.controller;

import com.secure.common.dto.ApiResponse;
import com.secure.order.dto.CreateOrderRequest;
import com.secure.order.dto.OrderDTO;
import com.secure.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received request to create order for user: {}", request.getUserId());
        try {
            String jwt = orderService.extractJwtFromSecurityContext();
            OrderDTO order = orderService.createOrder(request, jwt);
            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Order created successfully")
                    .data(order)
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Failed to create order: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getCurrentUserOrders(Authentication authentication) {
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            Long userId = jwt.getClaim("userId");

            log.info("Fetching orders for current user: {}", userId);
            List<OrderDTO> orders = orderService.getOrdersByUserId(userId);

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Orders retrieved successfully")
                    .data(orders)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching orders: {}", e.getMessage(), e);
            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Failed to fetch orders: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getOrderById(@PathVariable Long id) {
        log.info("Fetching order with ID: {}", id);
        try {
            OrderDTO order = orderService.getOrderById(id);
            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Order retrieved successfully")
                    .data(order)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching order: {}", e.getMessage(), e);
            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Failed to fetch order: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancelOrder(@PathVariable Long id) {
        log.info("Cancelling order with ID: {}", id);
        try {
            OrderDTO order = orderService.cancelOrder(id);
            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Order cancelled successfully")
                    .data(order)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cancelling order: {}", e.getMessage(), e);
            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Failed to cancel order: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

}
