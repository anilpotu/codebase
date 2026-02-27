package com.secure.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * Product Service Application
 * Demonstrates public endpoints, Redis caching, and mixed security
 */
@SpringBootApplication(scanBasePackages = {"com.secure.product", "com.secure.common"})
@EnableEurekaClient
@EnableCaching
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
