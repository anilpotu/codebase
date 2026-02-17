CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    description VARCHAR(500),
    transaction_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_transactions_account_id ON transactions (account_id);
