CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    surface VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    zone_name VARCHAR(100),
    scheduled_at TIMESTAMP NOT NULL,
    min_level INT,
    max_level INT,
    max_players INT NOT NULL,
    current_players INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_groups_category FOREIGN KEY (category_id) REFERENCES categories (id)
);
