-- Create users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);

-- Create roles table
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200)
);

-- Create user_roles join table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create refresh_tokens table
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insert seed data for roles
INSERT INTO roles (id, name, description) VALUES
    (1, 'ROLE_USER', 'Standard user role with basic permissions'),
    (2, 'ROLE_ADMIN', 'Administrator role with full permissions');

-- Insert seed data for users
-- Password for admin: admin123 (BCrypt hash with strength 12)
INSERT INTO users (id, username, email, password, enabled, account_non_locked, created_at) VALUES
    (1, 'admin', 'admin@secure.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5qdnvXz9cJL2K', TRUE, TRUE, CURRENT_TIMESTAMP);

-- Password for user: user123 (BCrypt hash with strength 12)
INSERT INTO users (id, username, email, password, enabled, account_non_locked, created_at) VALUES
    (2, 'user', 'user@secure.com', '$2a$12$wHdKmrBYXjXFKvvRxXZqKeOcFfeJPCMqZmLCVzXKpvJZ9RJXVqHrW', TRUE, TRUE, CURRENT_TIMESTAMP);

-- Assign roles to users
-- Admin user gets both ROLE_USER and ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 1),
    (1, 2);

-- Regular user gets ROLE_USER
INSERT INTO user_roles (user_id, role_id) VALUES
    (2, 1);
