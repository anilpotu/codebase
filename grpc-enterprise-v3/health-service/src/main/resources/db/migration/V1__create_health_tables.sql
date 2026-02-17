CREATE TABLE health_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    blood_type VARCHAR(10),
    height_cm DOUBLE PRECISION,
    weight_kg DOUBLE PRECISION,
    allergies TEXT,
    conditions TEXT,
    medications TEXT,
    last_checkup_date DATE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE vitals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    heart_rate INTEGER,
    systolic_bp INTEGER,
    diastolic_bp INTEGER,
    temperature_celsius DOUBLE PRECISION,
    oxygen_saturation INTEGER,
    recorded_at TIMESTAMP NOT NULL
);
