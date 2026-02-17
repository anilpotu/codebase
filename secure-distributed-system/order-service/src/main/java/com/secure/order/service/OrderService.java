package com.secure.order.service;

import com.secure.common.dto.UserDTO;
import com.secure.order.dto.CreateOrderRequest;
import com.secure.order.dto.OrderDTO;
import com.secure.order.dto.OrderItemDTO;
import com.secure.order.entity.Order;
import com.secure.order.entity.OrderItem;
import com.secure.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request, String jwt) {
        log.info("Creating order for user: {}", request.getUserId());

        // Validate user exists via Feign call
        try {
            String authHeader = "Bearer " + jwt;
            UserDTO user = userServiceClient.getUserById(request.getUserId(), authHeader);
            log.info("User validated: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to validate user: {}", e.getMessage());
            throw new RuntimeException("User validation failed: " + e.getMessage());
        }

        // Calculate total amount
        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create order
        Order order = Order.builder()
                .userId(request.getUserId())
                .status("PENDING")
                .totalAmount(totalAmount)
                .build();

        // Add items
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .price(itemRequest.getPrice())
                    .build();
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());

        return toDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        log.info("Fetching orders for user: {}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        log.info("Fetching order: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return toDTO(order);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        log.info("Cancelling order: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("Order is already cancelled");
        }

        order.setStatus("CANCELLED");
        Order updatedOrder = orderRepository.save(order);
        log.info("Order cancelled successfully: {}", orderId);

        return toDTO(updatedOrder);
    }

    public String extractJwtFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getTokenValue();
        }
        throw new RuntimeException("No JWT token found in security context");
    }

    private OrderDTO toDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> OrderItemDTO.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(itemDTOs)
                .createdAt(order.getCreatedAt())
                .build();
    }

}
