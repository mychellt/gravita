# UC-M1-15 — Allocate Document Number (`AllocateDocumentNumberUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

Doc §13 ("Numeração de séries"), referenced by §2.1: centralized numbering control with optimistic lock, to avoid duplicate document numbers in multi-user/multi-branch scenarios.

## Description

`tax` (M2/M3/M4) calls this use case immediately before queuing a fiscal document for transmission, to atomically reserve the next number in the company's configured series for that document type. Precondition: `ConfigureDocumentSeriesUseCase` (UC-04) has set up the series. Postcondition: a unique `(series, number)` pair is reserved and `DocumentSeries.nextNumber` is advanced — never allocated twice, even under concurrent issuance from multiple registers/branches.

## Port signature

```java
public interface AllocateDocumentNumberUseCase {
    DocumentNumber execute(AllocateDocumentNumberCommand command);
}
```

`AllocateDocumentNumberCommand`: `companyId`, `documentType: {NFE, NFCE, NFSE}`. Returns `DocumentNumber{series, number}`.

## Outbound ports required

- `DocumentSeriesRepositoryPort` (with optimistic-lock support, doc §13)

## REST endpoint

None — this is an internal inbound port called by `tax` (M2/M3/M4) at issuance time, not user-facing (per the module spec's use case table: "called by `tax`, not user-facing").

## Domain entities touched

- `DocumentSeries`

## Acceptance criteria

- [ ] Two concurrent calls for the same `(company, documentType)` never return the same number — enforced via optimistic locking with retry, not a database-wide lock that would block transmission (doc §13: "nunca bloquear a interface esperando resposta").
- [ ] On an optimistic-lock conflict, the use case retries internally rather than surfacing the conflict to the caller.
- [ ] Allocation and `DocumentSeries.nextNumber` advancement happen atomically — a crash between them must not produce a gap that later causes a duplicate.

## Dependencies

- **Depends on:** UC-04 (Configure Document Series).
- **Blocks:** All fiscal issuance use cases in M2, M3 and M4 — none can queue a document without a reserved number.
