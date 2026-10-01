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

## Implementation notes

- **Endpoint.** `POST /api/nfse/{id}/cancel` with body `{"justification": "..."}`; `204` with no body on success. Errors: `400` missing/blank `justification` (bean validation; the service re-checks it as well); `404` unknown id; `409` document not `AUTHORIZED` (a `DRAFT`, an RPS or an already `CANCELLED` one), no integration registered for the provider's municipality, municipality not homologated (cancellation must then be requested manually), homologated standard with no adapter, or the municipality **refused** the cancellation (its reason is in the body); `503` municipality did not answer.
- **Adapter (AC3).** `IssueNfsePort` gains `cancel(NfseCancellationRequest)` returning `NfseCancellationResult` (`confirmed(cancelledAt)` or `rejected(reason)`), so the same per-standard bean that issues an NFSe also cancels it - the use case looks it up by `standard()` exactly like `TransmitNfseService` (shared `IssueNfsePortRegistry`). As with issuance, it throws `NfseMunicipalityUnavailableException` when the municipality cannot be reached. No concrete adapter ships with this ticket (see UC-M4-04).
- **Transaction.** One transaction holding the row lock (`findByIdForUpdate`), so concurrent cancellations are serialized. The document is only saved after the municipality confirms; a refusal or an unreachable municipality rolls back and leaves it `AUTHORIZED`, so there is no intermediate state. Trade-off as in M4-04: a connection and row lock are held during the webservice call.
- **Domain (AC1, AC2, AC4).** `NfseDocument.cancel(justification, cancelledAt)` is the only `AUTHORIZED -> CANCELLED` transition and enforces both the status and a non-blank justification itself. The cancelled document keeps its protocol, authorization time and `xmlReference`; the row is updated, never deleted, and there is no delete operation on `NfseRepositoryPort`.
- **Time.** `cancelledAt` is the time reported by the municipality's confirmation.
- **No cancellation deadline.** Unlike NFe (M2-04), the spec sets no cancellation window; municipal deadlines differ and are enforced by the municipality, whose refusal surfaces as `409`.
- Migration `V75` adds `cancellation_justification` and `cancelled_at` to `nfse_documents`.
