package com.secure.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server Application for Service Discovery.
 * <p>
 * This application provides a centralized service registry where microservices
 * can register themselves and discover other services in the distributed system.
 * It includes security configuration to protect the Eureka dashboard and REST endpoints.
 * </p>
 *
 * @author Secure Distributed System Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    /**
     * Main method to start the Eureka Server application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
