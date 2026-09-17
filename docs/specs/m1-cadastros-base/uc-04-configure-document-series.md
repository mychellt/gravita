# UC-M1-04 — Configure Document Series (`ConfigureDocumentSeriesUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.1 Empresa / Filiais: document series — series and next number configured per company for NFe, NFCe and NFSe.

## Description

An administrator sets the series number and starting "next number" for a given document type (NFe, NFCe or NFSe) on a company. This is a one-time or occasional configuration step, distinct from the atomic per-issuance allocation done by UC-15. Precondition: the company exists (UC-01). Postcondition: the `DocumentSeries` for that `(company, documentType)` pair is ready for allocation.

## Port signature

```java
public interface ConfigureDocumentSeriesUseCase {
    void execute(ConfigureDocumentSeriesCommand command);
}
```

`ConfigureDocumentSeriesCommand`: `companyId`, `documentType: {NFE, NFCE, NFSE}`, `series`, `nextNumber`.

## Outbound ports required

- `DocumentSeriesRepositoryPort` (persistence with optimistic-lock support, doc §13)

## REST endpoint

`PUT /api/companies/{id}/document-series/{type}`

## Domain entities touched

- `DocumentSeries`
- `Company` (owner reference)

## Acceptance criteria

- [ ] Series/next-number are configured independently per document type (NFe, NFCe, NFSe don't share a counter).
- [ ] Reconfiguring `nextNumber` downward is rejected once numbers in that series have already been allocated, to prevent duplicates.
- [ ] Changes are visible to UC-15 (`AllocateDocumentNumberUseCase`) immediately, with no caching lag.

## Dependencies

- **Depends on:** UC-01 (Register Company).
- **Blocks:** UC-15 (Allocate Document Number), and therefore all fiscal issuance in M2/M3/M4.
