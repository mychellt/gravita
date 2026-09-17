# UC-M4-02 — Issue RPS (`IssueRpsUseCase`)

**Module:** M4 — Fiscal: NFSe ([module spec](../m4-fiscal-nfse.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 6 — Serviços (doc §14)

## Functional requirement

RPS: Recibo Provisório de Serviços issued internally; converted in batch or individually (§5.2). Service code validated against LC 116/2003 plus the municipal list (§5.2). ISS rate configurable per service and municipality, with manual override requiring justification (§5.2). Withholdings (ISS, PIS, COFINS, CSLL, IRPJ, INSS) activated by service type and recipient profile (§5.2).

## Description

A user (or an automated trigger from M7 invoicing) issues an internal RPS for a service rendered: selects the tomador (PF or PJ), the service code, place of provision (provider or recipient municipality), and the discrimination text (optionally pre-filled from a `DiscriminationTemplate`). The use case resolves the ISS rate and applicable withholdings via M2's shared `CalculateTaxUseCase`, allowing a manual override with a justification. The RPS is held internally, pending conversion to an authorized `NfseDocument` (UC-M4-03).

## Port signature

```java
public interface IssueRpsUseCase {
    RpsId execute(IssueRpsCommand command);
}
```

`IssueRpsCommand`: `provider` (CompanyRef), `tomador` (PersonRef), `serviceCode`, `placeOfProvision` (`PROVIDER` | `RECIPIENT`), `discrimination`, `issRateOverride` (optional, requires `overrideJustification` when set). Returns the created RPS id, in `DRAFT`-equivalent state ahead of conversion.

## Outbound ports required

- `NfseRepositoryPort` (RPS is persisted as a pre-conversion record within the same aggregate/repository as `NfseDocument`)

## REST endpoint

`POST /api/nfse/rps`

## Domain entities touched

- `NfseDocument` (its `rps{number, series}`, `serviceCode`, `placeOfProvision`, `tomador`, `issRate`, `withholdings`, `discrimination` fields)

## Acceptance criteria

- [ ] Service code is validated against the LC 116/2003 list and the tomador's municipality's list before the RPS is accepted.
- [ ] `tomador` requires a full address when withholding at source applies.
- [ ] ISS rate defaults to the configured per-service/per-municipality rate; a manual override is only accepted together with a justification.
- [ ] Withholdings (ISS, PIS, COFINS, CSLL, IRPJ, INSS) are computed based on service type and tomador profile, not hardcoded per call.
- [ ] An issued RPS has its own number/series, independent of the NFe series (`masterdata`'s `DocumentSeries`).

## Dependencies

- **Depends on:** M2 — `CalculateTaxUseCase` (../m2-fiscal-nfe/uc-02-calculate-tax.md), for ISS rate resolution and withholding computation.
- **Blocks:** UC-M4-03 (`convert-rps-to-nfse`).
