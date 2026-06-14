# ADR-001: Adopt Clean Architecture

## Status

Accepted

## Context

The project contains multiple technical concerns:

* HTTP request handling
* Database access
* Cryptographic operations
* Background processing
* Business rules

Without architectural boundaries, business logic would become tightly coupled to infrastructure implementations.

This would make testing difficult and increase maintenance costs.

## Decision

Adopt Clean Architecture.

The system is divided into:

* Domain Layer
* Application Layer
* Infrastructure Layer

Dependencies are directed inward.

Infrastructure implements application ports.

Business rules remain independent from technical details.

## Consequences

### Advantages

* Business logic can be tested independently.
* Infrastructure can be replaced with minimal changes.
* Clear separation of responsibilities.
* Better maintainability.

### Disadvantages

* Increased number of classes.
* Additional abstraction layers.
* Higher initial development effort.
