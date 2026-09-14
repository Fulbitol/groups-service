CREATE TABLE category_surfaces (
    category_id BIGINT NOT NULL,
    surface VARCHAR(50) NOT NULL,
    CONSTRAINT fk_category_surfaces_category FOREIGN KEY (category_id) REFERENCES categories (id)
);