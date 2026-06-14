# ADR-002: Use Transactional Outbox Pattern

## Status

Accepted

## Context

The system must create notifications after message processing.

A direct publish approach creates a consistency problem:

```text
Business data committed
Event publication failed
```

This leads to missing notifications.

## Decision

Store events in an Outbox table within the same transaction as business state changes.

A dedicated worker dispatches events asynchronously.

## Consequences

### Advantages

* Reliable event delivery.
* Atomic state transitions.
* Retry support.

### Disadvantages

* Additional database table.
* Additional worker process.
* Slightly increased operational complexity.
