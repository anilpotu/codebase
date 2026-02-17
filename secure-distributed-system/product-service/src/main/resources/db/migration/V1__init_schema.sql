-- Product Service - Initial Schema
-- Create products table

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL,
    category VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create categories table (optional)
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

-- Insert sample categories
INSERT INTO categories (name, description) VALUES
    ('Electronics', 'Electronic devices and accessories'),
    ('Books', 'Books and educational materials'),
    ('Clothing', 'Apparel and fashion items'),
    ('Home & Garden', 'Home improvement and gardening'),
    ('Sports', 'Sports equipment and accessories'),
    ('Toys', 'Toys and games');

-- Insert sample products
INSERT INTO products (name, description, price, stock_quantity, category, active, created_at, updated_at) VALUES
    ('Laptop Pro 15', 'High-performance laptop with 16GB RAM and 512GB SSD', 1299.99, 50, 'Electronics', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Wireless Mouse', 'Ergonomic wireless mouse with 6 programmable buttons', 29.99, 150, 'Electronics', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('USB-C Cable', 'Durable 6ft USB-C charging cable', 12.99, 300, 'Electronics', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Mechanical Keyboard', 'RGB mechanical keyboard with Cherry MX switches', 149.99, 75, 'Electronics', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('4K Monitor', '27-inch 4K UHD monitor with HDR support', 399.99, 40, 'Electronics', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('Java Programming Guide', 'Comprehensive guide to Java programming', 49.99, 100, 'Books', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Clean Code', 'A handbook of agile software craftsmanship', 42.99, 85, 'Books', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Design Patterns', 'Elements of reusable object-oriented software', 54.99, 60, 'Books', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('Cotton T-Shirt', 'Comfortable 100% cotton t-shirt', 19.99, 200, 'Clothing', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Denim Jeans', 'Classic fit denim jeans', 59.99, 120, 'Clothing', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Running Shoes', 'Lightweight running shoes with cushioned sole', 89.99, 80, 'Clothing', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('Garden Tools Set', 'Complete set of essential garden tools', 79.99, 45, 'Home & Garden', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('LED Desk Lamp', 'Adjustable LED desk lamp with touch control', 34.99, 110, 'Home & Garden', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('Basketball', 'Official size and weight basketball', 24.99, 90, 'Sports', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Yoga Mat', 'Non-slip yoga mat with carrying strap', 29.99, 130, 'Sports', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('Building Blocks Set', '500-piece creative building blocks', 39.99, 70, 'Toys', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Remote Control Car', 'High-speed RC car with rechargeable battery', 49.99, 55, 'Toys', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Create indexes for better query performance
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_name ON products(name);
