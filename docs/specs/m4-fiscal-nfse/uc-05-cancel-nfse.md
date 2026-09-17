# UC-M4-05 — Cancel NFSe (`CancelNfseUseCase`)

**Module:** M4 — Fiscal: NFSe ([module spec](../m4-fiscal-nfse.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 6 — Serviços (doc §14)

## Functional requirement

Cancellation: requested via the municipality's webservice; justification required (§5.2).

## Description

Cancels an `AUTHORIZED` `NfseDocument`. The request is submitted to the municipality's webservice through the same standard-specific `IssueNfsePort` adapter used for issuance, carrying a mandatory justification. On confirmation, the document moves to `CANCELLED`; the record itself is never deleted, consistent with fiscal immutability (doc §11.2, `system`).

## Port signature

```java
public interface CancelNfseUseCase {
    void execute(CancelNfseCommand command);
}
```

`CancelNfseCommand`: `nfseId`, `justification` (mandatory).

## Outbound ports required

- `NfseRepositoryPort`
- `IssueNfsePort` (cancellation request via the municipality's webservice)

## REST endpoint

`POST /api/nfse/{id}/cancel`

## Domain entities touched

- `NfseDocument` (`status` transition to `CANCELLED`)

## Acceptance criteria

- [ ] Only `AUTHORIZED` documents can be cancelled.
- [ ] A justification is mandatory and persisted with the cancellation.
- [ ] Cancellation is submitted via the municipality's webservice using the document's registered standard adapter.
- [ ] The document record is retained after cancellation — never physically deleted.

## Dependencies

- **Depends on:** UC-M4-04 (`transmit-nfse`) — only an authorized document can be cancelled.
- **Blocks:** —
