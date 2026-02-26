package com.secure.product.service;

import com.secure.product.dto.CreateProductRequest;
import com.secure.product.dto.ProductDTO;
import com.secure.product.dto.UpdateProductRequest;
import com.secure.product.entity.Product;
import com.secure.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Product Service with Redis Caching
 */
@Service
@Slf4j
@Transactional
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * Get all products with caching
     */
    @Cacheable("products")
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.info("Fetching all products from database");
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get product by ID with caching
     */
    @Cacheable(value = "product", key = "#id")
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        log.info("Fetching product with id: {} from database", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return convertToDTO(product);
    }

    /**
     * Create product and update cache
     */
    @CachePut(value = "product", key = "#result.id")
    public ProductDTO createProduct(CreateProductRequest request) {
        log.info("Creating new product: {}", request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with id: {}", savedProduct.getId());

        // Evict products list cache
        evictAllProductsCache();

        return convertToDTO(savedProduct);
    }

    /**
     * Update product and update cache
     */
    @CachePut(value = "product", key = "#id")
    public ProductDTO updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Update only non-null fields
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated with id: {}", updatedProduct.getId());

        // Evict products list cache
        evictAllProductsCache();

        return convertToDTO(updatedProduct);
    }

    /**
     * Delete product and evict from cache
     */
    @CacheEvict(value = "product", key = "#id")
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        productRepository.delete(product);
        log.info("Product deleted with id: {}", id);

        // Evict products list cache
        evictAllProductsCache();
    }

    /**
     * Search products by name
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String name) {
        log.info("Searching products with name containing: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get products by category
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        log.info("Fetching products by category: {}", category);
        return productRepository.findByCategory(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Decrement stock by the given quantity (called by order-service when an order is placed).
     * Validates the product is active and has sufficient stock before decrementing.
     */
    @CacheEvict(value = "product", key = "#productId")
    public ProductDTO decrementStock(Long productId, int quantity) {
        log.info("Decrementing stock for productId={} by quantity={}", productId, quantity);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new RuntimeException("Product is not active: " + productId);
        }
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for product " + productId
                    + ". Available: " + product.getStockQuantity() + ", requested: " + quantity);
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        Product updated = productRepository.save(product);
        log.info("Stock decremented for productId={}: new stock={}", productId, updated.getStockQuantity());

        evictAllProductsCache();
        return convertToDTO(updated);
    }

    /**
     * Evict all products list cache
     */
    @CacheEvict(value = "products", allEntries = true)
    public void evictAllProductsCache() {
        log.info("Evicting all products cache");
    }

    /**
     * Convert Product entity to ProductDTO
     */
    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .active(product.getActive())
                .build();
    }
}
