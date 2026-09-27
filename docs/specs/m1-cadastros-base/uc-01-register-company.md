# UC-M1-01 — Register Company (`RegisterCompanyUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.1 Empresa / Filiais: fiscal data (CNPJ, IE, IM, main CNAE, tax regime, Simples opt-in), contact data (address, issuing e-mail, phone, DANFE logo), and a data model prepared for multi-company (activated by config flag, no migration).

## Description

An administrator registers a new company (or branch of an existing one) with its fiscal and contact data. Registration also creates the company's initial `DocumentSeries` for NFe, NFCe and NFSe (empty series, to be configured via UC-04). Precondition: none — this is the first entity in the system. Postcondition: the company exists and can be referenced by every other M1–M10 use case.

## Port signature

```java
public interface RegisterCompanyUseCase {
    CompanyId execute(RegisterCompanyCommand command);
}
```

`RegisterCompanyCommand`: `cnpj: Document`, `ie`, `im`, `cnae`, `taxRegime`, `simplesOptante`, `address`, `state` (2-letter UF; GRA-96 — required for tax calculation and NFC-e/NFe access-key generation), `issuingEmail`, `phone`, `logoUrl`, `parentCompanyId` (optional, for a branch). Returns the new `CompanyId`.

## Outbound ports required

- `CompanyRepositoryPort`

## REST endpoint

`POST /api/companies` (also `PATCH /api/companies/{id}` for the update path, same use case).

## Domain entities touched

- `Company`
- `Branch`
- `DocumentSeries` (created empty, one per document type)

## Acceptance criteria

- [ ] CNPJ, IE and IM are validated before persisting.
- [ ] `state` is a valid 2-letter UF code (GRA-96).
- [ ] `taxRegime` accepts only `SIMPLES_NACIONAL`, `LUCRO_PRESUMIDO`, `LUCRO_REAL`.
- [ ] A `DocumentSeries` placeholder is created for NFe, NFCe and NFSe on registration.
- [ ] A branch can be registered against a `parentCompanyId` without requiring a schema migration (doc §2.1 multi-company).
- [ ] `sefazEnvironment` defaults to `HOMOLOGATION` until explicitly switched (UC-03).

## Dependencies

- **Depends on:** None.
- **Blocks:** Every other M1 ticket (all reference a `Company`), and all of M2/M3/M4/M6/M7/M8 (nothing can issue a fiscal document, buy, sell or invoice without a company).
