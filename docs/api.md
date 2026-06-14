# API Documentation

## Overview

ThreadBridge Secure exposes a minimal HTTP API for secure message delivery and retrieval.

Base URL:

```text
http://localhost:8080
```

---

# Submit Secure Message

Accepts a secure message request.

## Request

```http
POST /webhook/secure-messages
Content-Type: application/json
```

Body:

```json
{
  "requestId": "msg-001",
  "recipientId": 2,
  "text": "Hello secure world",
  "ttlSeconds": 3600,
  "oneTime": false
}
```

### Fields

| Field       | Description               |
| ----------- | ------------------------- |
| requestId   | Unique request identifier |
| recipientId | Target user               |
| text        | Message content           |
| ttlSeconds  | Message lifetime          |
| oneTime     | Destroy after first read  |

---

## Response

```http
202 Accepted
```

Body:

```json
{
  "status": "accepted"
}
```

---

# Get Inbox

Returns secure messages available to a recipient.

## Request

```http
GET /me/secure-messages
X-User-Id: 2
```

---

## Response

```json
[
  {
    "messageId": 101,
    "senderId": 1,
    "status": "DELIVERED",
    "oneTime": false,
    "createdAt": "2026-01-01T10:00:00",
    "expiresAt": "2026-01-01T11:00:00"
  }
]
```

---

# Decrypt Message

Decrypts a secure message.

## Request

```http
POST /secure-messages/{messageId}/decrypt
X-User-Id: 2
```

Example:

```http
POST /secure-messages/101/decrypt
```

---

## Response

```json
{
  "messageId": 101,
  "senderId": 1,
  "plaintext": "Hello secure world",
  "destroyedAfterRead": false
}
```

---

# Error Responses

## Validation Error

```http
400 Bad Request
```

```json
{
  "error": "Message has expired"
}
```

---

## Not Found

```http
404 Not Found
```

```json
{
  "error": "Secure message not found"
}
```

---

# Processing Model

Message submission is asynchronous.

Request flow:

```text
Client
  │
  ▼
Webhook
  │
  ▼
Inbound Queue
  │
  ▼
Background Encryption Worker
  │
  ▼
Secure Message Storage
```

A successful HTTP response only indicates acceptance of the request.
