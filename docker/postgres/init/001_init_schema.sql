CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(100) NOT NULL UNIQUE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       status VARCHAR(30) NOT NULL
);

CREATE TABLE user_keys (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL REFERENCES users(id),
                           public_key_pem TEXT NOT NULL,
                           encrypted_private_key_pem TEXT NULL,
                           key_fingerprint VARCHAR(128) NOT NULL UNIQUE,
                           algorithm VARCHAR(50) NOT NULL,
                           status VARCHAR(30) NOT NULL,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           activated_at TIMESTAMP NULL,
                           revoked_at TIMESTAMP NULL
);
CREATE TABLE inbound_secure_messages (
                             id BIGSERIAL PRIMARY KEY,
                             request_id VARCHAR(150) NOT NULL UNIQUE,
                             sender_id BIGINT NOT NULL REFERENCES users(id),
                             recipient_id BIGINT NOT NULL REFERENCES users(id),
                             plaintext_payload TEXT NOT NULL,
                             ttl_seconds INTEGER NOT NULL,
                             one_time BOOLEAN NOT NULL,
                             status VARCHAR(30) NOT NULL,
                             failure_reason TEXT NULL,
                             received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             processing_started_at TIMESTAMP NULL,
                             processed_at TIMESTAMP NULL
);
CREATE TABLE secure_messages (
                                 id BIGSERIAL PRIMARY KEY,
                                 inbound_message_id BIGINT NOT NULL UNIQUE REFERENCES inbound_secure_messages(id),
                                 sender_id BIGINT NOT NULL REFERENCES users(id),
                                 recipient_id BIGINT NOT NULL REFERENCES users(id),
                                 recipient_key_id BIGINT NOT NULL REFERENCES user_keys(id),
                                 encrypted_payload TEXT NOT NULL,
                                 encrypted_content_key TEXT NOT NULL,
                                 nonce TEXT NOT NULL,
                                 algorithm VARCHAR(100) NOT NULL,
                                 status VARCHAR(30) NOT NULL,
                                 one_time BOOLEAN NOT NULL,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 expires_at TIMESTAMP NOT NULL,
                                 read_at TIMESTAMP NULL,
                                 destroyed_at TIMESTAMP NULL
);
CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL REFERENCES users(id),
                               secure_message_id BIGINT REFERENCES secure_messages(id),
                               type VARCHAR(50) NOT NULL,
                               status VARCHAR(30) NOT NULL,
                               text TEXT NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               read_at TIMESTAMP NULL
);
CREATE TABLE audit_events (
                              id BIGSERIAL PRIMARY KEY,
                              actor_user_id BIGINT NULL REFERENCES users(id),
                              event_type VARCHAR(100) NOT NULL,
                              aggregate_type VARCHAR(100) NOT NULL,
                              aggregate_id BIGINT NULL,
                              details TEXT NULL,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_events (
                               id BIGSERIAL PRIMARY KEY,
                               event_type VARCHAR(100) NOT NULL,
                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id BIGINT NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(30) NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               processing_started_at TIMESTAMP NULL,
                               processed_at TIMESTAMP NULL,
                               failure_reason TEXT NULL
);


CREATE INDEX ix_inbound_status_received
    ON inbound_secure_messages(status, received_at);

CREATE INDEX ix_secure_messages_recipient_status
    ON secure_messages(recipient_id, status, created_at DESC);

CREATE INDEX ix_secure_messages_expires_at
    ON secure_messages(expires_at);

CREATE INDEX ix_notifications_user_status
    ON notifications(user_id, status, created_at DESC);

CREATE INDEX ix_outbox_status_created_at
    ON outbox_events(status, created_at);

CREATE UNIQUE INDEX IF NOT EXISTS ux_notifications_secure_message_type
    ON notifications(secure_message_id, type);