-- Create user_profiles table
CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    address VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create index on user_id for faster lookups
CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);

-- Insert sample data
INSERT INTO user_profiles (user_id, first_name, last_name, phone_number, address, created_at, updated_at)
VALUES
    (1, 'John', 'Doe', '+1-555-0100', '123 Main Street, New York, NY 10001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Jane', 'Smith', '+1-555-0101', '456 Oak Avenue, Los Angeles, CA 90001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Bob', 'Johnson', '+1-555-0102', '789 Elm Street, Chicago, IL 60601', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
