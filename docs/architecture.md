# Architecture

## Architectural Goals

ThreadBridge Secure was designed as a study of production-oriented backend architecture.

Primary goals:

* Separation of business logic from infrastructure.
* Reliable asynchronous processing.
* Secure message storage.
* Idempotent request handling.
* Extensibility without modification of core business logic.
* Clear dependency boundaries.

---

# Architectural Style

The system follows Clean Architecture principles.

```text
Presentation Layer
        │
        ▼
Application Layer
        │
        ▼
Domain Layer

Infrastructure depends on all layers.
Domain depends on none.
```

Dependency direction always points inward.

Business rules must never depend on:

* JDBC
* PostgreSQL
* Docker
* HTTP
* JSON libraries
* Thread pools

---

# Layer Responsibilities

## Domain Layer

Location:

```text
domain/
```

Contains:

* Entities
* Value Objects
* Domain Rules
* Domain Invariants

Examples:

```text
SecureMessage
UserKey
Notification
OutboxEvent

SecureMessageStatus
NotificationStatus
AuditEventType
```

The domain layer contains no infrastructure code.

No SQL.

No HTTP.

No JSON.

No framework dependencies.

---

## Application Layer

Location:

```text
application/
```

Contains:

```text
usecase/
port/
command/
result/
service/
```

Responsibilities:

* Application workflows
* Transaction boundaries
* Coordination of repositories
* Coordination of external services

Examples:

```text
AcceptInboundSecureMessageUseCase
EncryptSecureMessageUseCase
DecryptSecureMessageUseCase
GetSecureInboxUseCase
```

Use Cases orchestrate business scenarios.

They do not contain persistence implementation details.

---

## Infrastructure Layer

Location:

```text
infrastructure/
```

Contains:

```text
jdbc/
worker/
cache/
crypto/
event/
web/
```

Responsibilities:

* Database access
* Cryptography implementation
* Background processing
* HTTP transport
* Event dispatching

Infrastructure implements application ports.

---

# Message Processing Pipeline

The system processes incoming messages asynchronously.

```text
Webhook Request
        │
        ▼
Inbound Message
        │
        ▼
Inbound Poller
        │
        ▼
EncryptSecureMessageUseCase
        │
        ▼
SecureMessage
        │
        ▼
Outbox Event
        │
        ▼
Outbox Dispatcher
        │
        ▼
Notification Creation
```

This design prevents expensive cryptographic operations from blocking HTTP request processing.

---

# Idempotency

Incoming requests contain a unique request identifier.

```text
requestId
```

Before processing a request:

```text
1. Check existing requestId.
2. If exists → ignore.
3. If absent → create new inbound record.
```

Benefits:

* Safe retries.
* No duplicate message creation.
* No duplicate notifications.

---

# Transactional Outbox Pattern

The system uses the Transactional Outbox Pattern.

Problem:

```text
Database updated
Event publication failed
```

Result:

```text
Inconsistent state
```

Solution:

```text
1. Save business data.
2. Save outbox event.
3. Commit transaction.
4. Dispatcher publishes event later.
```

Both records are stored atomically.

This guarantees eventual event delivery.

---

# Worker Architecture

The system uses dedicated background workers.

Workers:

```text
InboundSecureMessagePoller
OutboxDispatcherWorker
CleanupExpiredMessagesWorker
```

Each worker:

```text
claim batch
process
commit
sleep
repeat
```

Benefits:

* Isolation of workloads.
* Controlled throughput.
* Reduced request latency.

---

# Cryptographic Architecture

ThreadBridge Secure uses hybrid encryption.

Reason:

RSA is expensive for large payloads.

AES is efficient for content encryption.

Solution:

```text
Message
    │
    ▼
AES-256-GCM
    │
    ▼
Ciphertext

AES Key
    │
    ▼
RSA Public Key
    │
    ▼
Encrypted AES Key
```

Stored data:

```text
encrypted_payload
encrypted_content_key
nonce
algorithm
```

Plaintext is never persisted.

---

# Secure Message Lifecycle

```text
DELIVERED
    │
    ├── decrypt
    ▼
READ

DELIVERED
    │
    ├── one-time decrypt
    ▼
DESTROYED

DELIVERED
    │
    ├── ttl expiration
    ▼
EXPIRED
```

State transitions are enforced by domain rules.

---

# Caching Strategy

Inbox retrieval uses cache-first access.

```text
GET Inbox
    │
    ▼
Cache
    │
    ├── hit
    ▼
Return Data

    └── miss
            │
            ▼
        Database
            │
            ▼
         Populate Cache
```

Cache invalidation occurs through event processing.

---

# Concurrency Strategy

The system processes data concurrently.

Concurrency controls:

* Database row locking.
* FOR UPDATE SKIP LOCKED.
* Reader-Writer cache lock.
* Worker isolation.

Goals:

* Prevent duplicate processing.
* Prevent lost updates.
* Support parallel workers.

---

# Audit Strategy

Security-sensitive operations generate audit records.

Examples:

```text
INBOUND_ACCEPTED
MESSAGE_ENCRYPTED
MESSAGE_READ
MESSAGE_EXPIRED
DECRYPT_DENIED
```

Audit events provide traceability and security visibility.

---

# Trade-Offs

Advantages:

* Strong separation of concerns.
* Reliable event delivery.
* Easy testing of business logic.
* High maintainability.

Disadvantages:

* More classes.
* More infrastructure code.
* Additional operational complexity.

The trade-off was accepted in exchange for maintainability and reliability.

---

# Future Evolution

Potential future improvements:

* Distributed event bus.
* Kafka integration.
* OpenTelemetry tracing.
* Prometheus metrics.
* Distributed cache.
* Key rotation support.
* Authentication and authorization layer.
