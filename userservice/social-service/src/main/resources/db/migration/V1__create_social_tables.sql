CREATE TABLE social_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    bio TEXT,
    location VARCHAR(255),
    website VARCHAR(255),
    followers_count INTEGER NOT NULL DEFAULT 0,
    following_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    likes_count INTEGER NOT NULL DEFAULT 0,
    comments_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP
);

CREATE TABLE connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    connected_user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT uk_user_connected_user UNIQUE (user_id, connected_user_id)
);

CREATE INDEX idx_posts_user_id ON posts (user_id);
CREATE INDEX idx_connections_user_id_status ON connections (user_id, status);
