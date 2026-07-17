CREATE TABLE orders (
    id VARCHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(100) UNIQUE,
    symbol VARCHAR(20) NOT NULL,
    side VARCHAR(10) NOT NULL,
    type VARCHAR(10) NOT NULL,
    price DECIMAL(18, 8),
    quantity DECIMAL(18, 8) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE trades (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    price DECIMAL(18, 8) NOT NULL,
    quantity DECIMAL(18, 8) NOT NULL,
    executed_at TIMESTAMP NOT NULL
);

CREATE TABLE positions (
    symbol VARCHAR(20) PRIMARY KEY,
    quantity DECIMAL(18, 8) NOT NULL,
    average_price DECIMAL(18, 8) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
