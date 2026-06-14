# ThreadBridge Secure

## Overview

ThreadBridge Secure is a secure message delivery platform built using Clean Architecture principles and asynchronous processing patterns.

The system accepts incoming messages through a webhook API, encrypts them using hybrid cryptography (AES + RSA), stores encrypted payloads, and delivers notifications through an event-driven pipeline.

The project was developed as a study of:

* Clean Architecture
* Domain-Driven Design concepts
* Asynchronous processing
* Transactional Outbox Pattern
* Idempotency
* Concurrency control
* Secure message handling
* Hybrid cryptography

---

## Key Features

### Secure Messaging

* Hybrid encryption (AES-256 + RSA)
* One-time messages
* Message expiration (TTL)
* Secure message decryption
* Audit trail

### Reliability

* Transactional Outbox Pattern
* Idempotent webhook processing
* Retry-friendly event delivery
* Worker-based asynchronous processing

### Performance

* Concurrent worker processing
* Reader-Writer inbox cache
* Batch processing
* Connection pooling

### Security

* RSA public/private key infrastructure
* AES content encryption
* Message expiration policies
* Read-once message support
* Audit logging

---

## Architecture

The system follows Clean Architecture principles.

```text
Webhook API
    │
    ▼
Inbound Message Storage
    │
    ▼
Inbound Poller
    │
    ▼
Encryption Use Case
    │
    ▼
Secure Message Storage
    │
    ▼
Outbox Event
    │
    ▼
Outbox Dispatcher
    │
    ▼
Notification Processing
```

---

## Project Structure

```text
src/main/java/mrk

├── application
│   ├── command
│   ├── port
│   ├── result
│   ├── service
│   └── usecase
│
├── domain
│   ├── model
│   └── value
│
├── infrastructure
│   ├── cache
│   ├── crypto
│   ├── jdbc
│   ├── worker
│   ├── event
│   └── web
│
└── bootstrap
```

---

## Core Architectural Patterns

### Clean Architecture

Business rules are isolated from infrastructure concerns.

### Transactional Outbox

Domain state changes and event publishing are performed atomically.

### Repository Pattern

Persistence logic is isolated behind application ports.

### Factory Pattern

Domain events and audit records are created through dedicated factories.

### Worker Pattern

Background processing is handled by dedicated worker threads.

### Idempotency Pattern

Duplicate webhook requests are safely ignored.

---

## Technology Stack

* Java 21
* PostgreSQL
* Docker
* JDBC
* HikariCP
* RSA
* AES-GCM
* Maven

---

## Running Locally

### Build

```bash
mvn clean package
```

### Start Infrastructure

```bash
docker compose up --build
```

### Run Application

```bash
java -jar target/threadBridgeSecure.jar
```

---

## Load Testing

Example:

```bash
bash scripts/load-test.sh
```

Environment variables:

```bash
BASE_URL=http://localhost:8080
TOTAL_REQUESTS=100
CONCURRENCY=10
```

---

## Security Model

### Encryption Flow

1. Generate random AES key.
2. Encrypt message content with AES-GCM.
3. Encrypt AES key using recipient RSA public key.
4. Store only encrypted data.

### Decryption Flow

1. Load encrypted payload.
2. Decrypt AES key using recipient private key.
3. Decrypt content using AES key.
4. Apply one-time and expiration policies.

---

## Future Improvements

* Dead Letter Queue
* Distributed cache
* Prometheus metrics
* OpenTelemetry tracing
* Multi-node deployment
* Key rotation
* REST authentication layer

---

## Learning Goals

This project was built to explore practical backend engineering concepts:

* System design
* Concurrent programming
* Secure data processing
* Event-driven architecture
* Reliable message delivery
* Production-oriented backend development
