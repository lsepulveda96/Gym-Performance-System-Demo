-- Gym System Database Schema
-- Compatible with PostgreSQL / Supabase

CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE plans (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    duration_days INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE member_profiles (
    user_id VARCHAR(36) PRIMARY KEY REFERENCES users(id),
    phone VARCHAR(50),
    join_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    current_plan_id VARCHAR(36) REFERENCES plans(id)
);

CREATE TABLE subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id),
    plan_id VARCHAR(36) REFERENCES plans(id),
    start_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE check_ins (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id),
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    plan_id_at_time VARCHAR(36) REFERENCES plans(id)
);

CREATE TABLE payments (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id),
    amount DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    plan_id VARCHAR(36) REFERENCES plans(id),
    reference VARCHAR(255)
);
