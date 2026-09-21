CREATE TABLE access_log (
    id          UUID PRIMARY KEY,
    user_id     UUID REFERENCES users (id),
    email       VARCHAR(255) NOT NULL,
    event       VARCHAR(10) NOT NULL CHECK (event IN ('LOGIN', 'LOGOUT')),
    successful  BOOLEAN NOT NULL,
    ip          VARCHAR(64),
    device      VARCHAR(255),
    timestamp   TIMESTAMP NOT NULL
);

CREATE INDEX idx_access_log_user_id ON access_log (user_id);
CREATE INDEX idx_access_log_timestamp ON access_log (timestamp);
