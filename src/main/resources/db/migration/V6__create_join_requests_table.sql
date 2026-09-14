CREATE TABLE join_requests (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    requested_position VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_join_requests_group FOREIGN KEY (group_id) REFERENCES groups (id)
);