# ADR-003: Enforce Idempotent Webhook Processing

## Status

Accepted

## Context

Webhook providers may resend requests.

Network failures can result in duplicate deliveries.

Duplicate processing would create:

* Duplicate messages.
* Duplicate notifications.
* Inconsistent audit records.

## Decision

Each incoming request contains a unique requestId.

The system stores request identifiers and rejects duplicates.

## Consequences

### Advantages

* Safe retries.
* Deterministic processing.
* Protection from duplicate message creation.

### Disadvantages

* Additional database lookup.
* Additional unique constraint.
