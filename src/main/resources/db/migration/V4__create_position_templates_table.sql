CREATE TABLE position_templates (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    position VARCHAR(50) NOT NULL,
    expected_count INT NOT NULL,
    CONSTRAINT fk_position_templates_category FOREIGN KEY (category_id) REFERENCES categories (id)
);