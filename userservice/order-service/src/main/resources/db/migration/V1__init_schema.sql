-- Create orders table
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
);

-- Create order_items table
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_price CHECK (price >= 0)
);

-- Create indexes for better query performance
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- Insert sample data
INSERT INTO orders (user_id, status, total_amount, created_at, updated_at)
VALUES
    (1, 'CONFIRMED', 299.98, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'PENDING', 549.97, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample order items for order 1
INSERT INTO order_items (order_id, product_id, quantity, price)
VALUES
    (1, 101, 2, 99.99),
    (1, 102, 1, 100.00);

-- Insert sample order items for order 2
INSERT INTO order_items (order_id, product_id, quantity, price)
VALUES
    (2, 103, 3, 149.99),
    (2, 104, 1, 49.99);
