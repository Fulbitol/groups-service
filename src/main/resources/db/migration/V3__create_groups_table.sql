CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    city VARCHAR(100) NOT NULL,
    location VARCHAR(150),
    min_level INT,
    max_level INT,
    max_players INT NOT NULL,
    current_players INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_groups_category FOREIGN KEY (category_id) REFERENCES categories (id)
);