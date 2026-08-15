CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_email
    ON users(email);


CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    transaction_reference VARCHAR(50) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    source_account VARCHAR(100) NOT NULL,
    destination_account VARCHAR(100) NOT NULL,
    transaction_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_transaction_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_transactions_user_id
    ON transactions(user_id);

CREATE INDEX idx_transactions_created_at
    ON transactions(created_at);

CREATE INDEX idx_transactions_status
    ON transactions(status);


CREATE TABLE fraud_predictions (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE,
    xgboost_probability NUMERIC(6, 5),
    xgboost_prediction BOOLEAN,
    isolation_score NUMERIC(10, 6),
    isolation_anomaly BOOLEAN,
    risk_score NUMERIC(6, 2),
    risk_level VARCHAR(20),
    model_version VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_prediction_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id)
);


CREATE INDEX idx_predictions_transaction_id
    ON fraud_predictions(transaction_id);

CREATE INDEX idx_predictions_created_at
    ON fraud_predictions(created_at);


CREATE TABLE fraud_rule_results (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_result BOOLEAN NOT NULL,
    severity VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_rule_result_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id)
);

CREATE INDEX idx_rule_results_transaction_id
    ON fraud_rule_results(transaction_id);


CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_audit_user_id
    ON audit_logs(user_id);

CREATE INDEX idx_audit_created_at
    ON audit_logs(created_at);