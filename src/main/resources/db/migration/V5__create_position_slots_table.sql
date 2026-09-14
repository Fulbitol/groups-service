CREATE TABLE position_slots (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    position VARCHAR(50) NOT NULL,
    expected_count INT NOT NULL,
    filled_slots INT NOT NULL,
    CONSTRAINT fk_position_slots_group FOREIGN KEY (group_id) REFERENCES groups (id)
);