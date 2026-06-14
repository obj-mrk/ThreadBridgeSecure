# Operations Runbook

## Purpose

This document describes operational procedures for ThreadBridge Secure.

---

# Startup

Build application:

```bash
mvn clean package
```

Start infrastructure:

```bash
docker compose up --build
```

Run application:

```bash
java -jar target/threadBridgeSecure.jar
```

---

# Verify System Health

Check application logs.

Expected workers:

```text
InboundSecureMessagePoller
OutboxDispatcherWorker
CleanupExpiredMessagesWorker
```

Expected startup messages:

```text
Application started
Worker started
Dispatcher started
```

---

# Database Diagnostics

## Inbound Messages

```sql
SELECT status, COUNT(*)
FROM inbound_secure_messages
GROUP BY status;
```

Expected:

```text
RECEIVED
PROCESSING
PROCESSED
FAILED
```

---

## Secure Messages

```sql
SELECT status, COUNT(*)
FROM secure_messages
GROUP BY status;
```

Expected:

```text
DELIVERED
READ
DESTROYED
EXPIRED
```

---

## Outbox Events

```sql
SELECT status, COUNT(*)
FROM outbox_events
GROUP BY status;
```

Expected:

```text
NEW
PROCESSING
PROCESSED
FAILED
```

---

## Notifications

```sql
SELECT status, COUNT(*)
FROM notifications
GROUP BY status;
```

Expected:

```text
NEW
SENT
FAILED
```

---

# Audit Verification

```sql
SELECT event_type, COUNT(*)
FROM audit_events
GROUP BY event_type;
```

Expected events:

```text
INBOUND_ACCEPTED
MESSAGE_ENCRYPTED
MESSAGE_READ
MESSAGE_EXPIRED
DECRYPT_DENIED
```

---

# Troubleshooting

## Messages Stuck In RECEIVED

Possible causes:

* Poller not running.
* Database connection issues.
* Worker startup failure.

Verify:

```sql
SELECT *
FROM inbound_secure_messages
WHERE status = 'RECEIVED';
```

---

## Messages Stuck In PROCESSING

Possible causes:

* Worker crash.
* Cryptography failure.
* Invalid public key.

Verify logs for:

```text
Could not parse RSA public key
Encryption failed
```

---

## Outbox Events Not Processed

Verify:

```sql
SELECT *
FROM outbox_events
WHERE status <> 'PROCESSED';
```

Check dispatcher logs.

---

## Notification Problems

Verify:

```sql
SELECT *
FROM notifications
ORDER BY created_at DESC;
```

---

# Load Testing

Run:

```bash
bash scripts/load-test.sh
```

Environment variables:

```bash
BASE_URL=http://localhost:8080
TOTAL_REQUESTS=100
CONCURRENCY=10
```

After completion verify:

```sql
SELECT status, COUNT(*)
FROM inbound_secure_messages
GROUP BY status;
```

---

# Recovery Procedures

## Reset Development Environment

Stop containers:

```bash
docker compose down -v
```

Start again:

```bash
docker compose up --build
```

---

# Operational Checklist

Before release:

* All workers running.
* Database healthy.
* Outbox empty.
* Notifications processing.
* Audit events generated.
* Load test completed.
* Integration scenarios verified.
