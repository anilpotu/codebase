package com.secure.product.controller;

import com.secure.common.dto.ApiResponse;
import com.secure.product.dto.CreateProductRequest;
import com.secure.product.dto.ProductDTO;
import com.secure.product.dto.UpdateProductRequest;
import com.secure.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Product Controller
 * Public GET endpoints, Authenticated POST/PUT/DELETE endpoints
 */
@RestController
@RequestMapping("/products")
@Slf4j
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * Get all products (PUBLIC)
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllProducts() {
        log.info("GET /products - Fetching all products");
        List<ProductDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Products retrieved successfully")
                        .data(products)
                        .build()
        );
    }

    /**
     * Get product by ID (PUBLIC)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getProductById(@PathVariable Long id) {
        log.info("GET /products/{} - Fetching product by id", id);
        try {
            ProductDTO product = productService.getProductById(id);
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Product retrieved successfully")
                            .data(product)
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Product not found with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
                    );
        }
    }

    /**
     * Search products by name (PUBLIC)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchProducts(@RequestParam String name) {
        log.info("GET /products/search?name={} - Searching products", name);
        List<ProductDTO> products = productService.searchProducts(name);
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Search completed successfully")
                        .data(products)
                        .build()
        );
    }

    /**
     * Get products by category (PUBLIC)
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse> getProductsByCategory(@PathVariable String category) {
        log.info("GET /products/category/{} - Fetching products by category", category);
        List<ProductDTO> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Products retrieved successfully")
                        .data(products)
                        .build()
        );
    }

    /**
     * Create product (ADMIN only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("POST /products - Creating new product: {}", request.getName());
        try {
            ProductDTO product = productService.createProduct(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.builder()
                            .success(true)
                            .message("Product created successfully")
                            .data(product)
                            .build()
                    );
        } catch (Exception e) {
            log.error("Error creating product", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Error creating product: " + e.getMessage())
                            .build()
                    );
        }
    }

    /**
     * Decrement product stock by quantity (internal — called by order-service)
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse> decrementStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        log.info("PATCH /products/{}/stock?quantity={} - Decrementing stock", id, quantity);
        try {
            ProductDTO product = productService.decrementStock(id, quantity);
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Stock decremented successfully")
                            .data(product)
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Error decrementing stock for product id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
                    );
        }
    }

    /**
     * Update product (ADMIN only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("PUT /products/{} - Updating product", id);
        try {
            ProductDTO product = productService.updateProduct(id, request);
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Product updated successfully")
                            .data(product)
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Error updating product with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
                    );
        } catch (Exception e) {
            log.error("Error updating product", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Error updating product: " + e.getMessage())
                            .build()
                    );
        }
    }

    /**
     * Delete product (ADMIN only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long id) {
        log.info("DELETE /products/{} - Deleting product", id);
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Product deleted successfully")
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Error deleting product with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
                    );
        } catch (Exception e) {
            log.error("Error deleting product", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Error deleting product: " + e.getMessage())
                            .build()
                    );
        }
    }
}
