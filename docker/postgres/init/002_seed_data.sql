INSERT INTO users (id, username, status)
VALUES
    (1, 'alice', 'ACTIVE'),
    (2, 'bob', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- Синхронизируем sequence после ручной вставки id.
SELECT setval(
               pg_get_serial_sequence('users', 'id'),
               COALESCE((SELECT MAX(id) FROM users), 1),
               true
       );

INSERT INTO user_keys (
    id,
    user_id,
    public_key_pem,
    encrypted_private_key_pem,
    key_fingerprint,
    algorithm,
    status,
    created_at,
    activated_at,
    revoked_at
)
VALUES
    (
        1,
        1,
        '-----BEGIN PUBLIC KEY-----
DEV_ONLY_ALICE_PUBLIC_KEY_PLACEHOLDER
-----END PUBLIC KEY-----',
        NULL,
        'dev-alice-key-fingerprint-001',
        'RSA-OAEP-SHA256',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        NULL
    ),
    (
        2,
        2,
        '-----BEGIN PUBLIC KEY-----
DEV_ONLY_BOB_PUBLIC_KEY_PLACEHOLDER
-----END PUBLIC KEY-----',
        NULL,
        'dev-bob-key-fingerprint-001',
        'RSA-OAEP-SHA256',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        NULL
    )
ON CONFLICT (id) DO NOTHING;

-- Синхронизируем sequence после ручной вставки id.
SELECT setval(
               pg_get_serial_sequence('user_keys', 'id'),
               COALESCE((SELECT MAX(id) FROM user_keys), 1),
               true
       );