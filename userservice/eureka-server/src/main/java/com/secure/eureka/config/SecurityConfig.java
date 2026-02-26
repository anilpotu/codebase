package com.secure.eureka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security Configuration for Eureka Server.
 * <p>
 * This configuration provides basic authentication for the Eureka dashboard
 * and REST endpoints while allowing health check endpoints to be accessed
 * without authentication for monitoring purposes.
 * </p>
 * <p>
 * CSRF protection is disabled for Eureka endpoints to allow seamless
 * service registration and discovery.
 * </p>
 *
 * @author Secure Distributed System Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * Configures HTTP security for the Eureka Server.
     * <p>
     * Configuration includes:
     * <ul>
     *   <li>CSRF disabled for Eureka endpoints</li>
     *   <li>Public access to health check endpoints</li>
     *   <li>Basic authentication required for all other endpoints</li>
     * </ul>
     * </p>
     *
     * @param http the HttpSecurity to configure
     * @throws Exception if an error occurs during configuration
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf()
                .disable()
            .authorizeRequests()
                .antMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                .anyRequest()
                    .authenticated()
            .and()
            .httpBasic();
    }

    /**
     * Creates a BCrypt password encoder bean.
     * <p>
     * BCrypt is a strong hashing algorithm designed for password encryption.
     * It includes a salt to protect against rainbow table attacks and is
     * computationally intensive to defend against brute-force attacks.
     * </p>
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
