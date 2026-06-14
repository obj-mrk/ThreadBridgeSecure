# ADR-004: Use Hybrid Encryption (AES + RSA)

## Status

Accepted

## Context

Secure messages must remain confidential.

RSA alone is computationally expensive for message payload encryption.

AES alone requires secure key exchange.

## Decision

Use hybrid encryption.

Process:

1. Generate random AES key.
2. Encrypt content using AES-GCM.
3. Encrypt AES key using recipient RSA public key.
4. Store encrypted payload and encrypted key.

## Consequences

### Advantages

* Strong confidentiality.
* Efficient encryption of large payloads.
* Industry-standard approach.

### Disadvantages

* More complex implementation.
* Key management requirements.
* Additional cryptographic components.
