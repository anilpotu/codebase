package com.secure.order.service;

import com.secure.common.dto.ApiResponse;
import com.secure.order.dto.ProductInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for product-service.
 * Used by order-service to validate product existence/stock and to decrement stock
 * after a successful order placement.
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/products/{productId}")
    ApiResponse<ProductInfo> getProductById(@PathVariable("productId") Long productId);

    @PatchMapping("/products/{productId}/stock")
    ApiResponse<ProductInfo> decrementStock(
            @PathVariable("productId") Long productId,
            @RequestParam("quantity") int quantity
    );
}
