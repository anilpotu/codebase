package com.secure.product.repository;

import com.secure.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Product Repository
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find products by category
     */
    List<Product> findByCategory(String category);

    /**
     * Find all active products
     */
    List<Product> findByActiveTrue();

    /**
     * Search products by name (case-insensitive)
     */
    List<Product> findByNameContainingIgnoreCase(String name);
}
