# ADR-005: Use Background Worker Processing

## Status

Accepted

## Context

Cryptographic operations are significantly slower than simple HTTP request handling.

Performing encryption synchronously would increase request latency.

## Decision

Accept requests immediately.

Store incoming requests.

Process encryption asynchronously using background workers.

## Consequences

### Advantages

* Fast request acknowledgement.
* Better scalability.
* Workload isolation.

### Disadvantages

* Eventual consistency.
* Additional worker management.
* More operational complexity.
