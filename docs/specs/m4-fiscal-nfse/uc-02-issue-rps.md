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

## Implementation notes

- `POST /api/nfse/rps` answers `201` with `{"id": "<uuid>"}` (the `RpsId`). Validation errors on the body are `400`; business-rule failures (bad service code, missing address, override without justification, no ISS rate) are `409`; an unknown provider company is `404`.
- **Command shape.** The ticket's `provider (CompanyRef)` / `tomador (PersonRef)` are not enough to do the work, so the command also carries: `providerMunicipalityIbgeCode` (the company master data only holds a free-text address and UF, no IBGE code); `tomador` as `{personId?, document, personType, name, municipalityIbgeCode?, address?}` (same "own validated copy" approach as `NfeRecipient`; `personId` is only set for a registered customer); and `serviceAmount`, the base the ISS rate and withholdings are applied to.
- **RPS and `NfseDocument` are one aggregate.** An RPS is an `NfseDocument` with the new status `NfseStatus.RPS` (ahead of `DRAFT`); `RpsId` shares the document's UUID, so UC-M4-03 converts the same row. Persisted in `nfse_documents` + `nfse_withholdings` (no separate RPS table). NFSe `number`/`series` per company+municipality is assigned at conversion (M4-03), not here.
- **Own series (AC5).** `FiscalDocumentType` gains `RPS`; the number comes from `AllocateDocumentNumberUseCase` for `(company, RPS)`, so it never touches NFE/NFCE/NFSE. New companies get an unconfigured `RPS` placeholder like the other types (an existing company needs one, see migration `V72`), and the series must be configured before the first RPS, otherwise `DocumentSeriesNotFoundException`. The number is allocated last, after every validation, so a rejected RPS does not burn one.
- **Deviation from the ticket: `CalculateTaxUseCase` is not called.** The delivered M2 engine is keyed by product NCM × UF × regime × operation and only knows ICMS/IPI/PIS/COFINS/FCP, with no ISS, municipality, CSLL, IRPJ or INSS dimension and no way to take a service code. Instead `IssueRpsService` reuses the shared `TaxEngine` (the single percentage-of-base implementation) fed by a new parameterized table, `service_tax_rules` (`ServiceTaxRuleRepositoryPort`). `TaxType` gains `ISS`, `CSLL`, `IRPJ`, `INSS` (appended, ordinals unchanged). Folding services into `CalculateTaxUseCase` itself would mean adding a municipality/service key to `TaxRateQuery` and `tax_rate_rules`; left for a follow-up if wanted.
- **Rule table.** One row = one tax for a service code, optionally narrowed to a municipality and/or the provider's tax regime (`NULL` = any), with a `withholding` mode (`NEVER`, `TOMADOR_COMPANY` = retained only when the tomador is a PJ, `ALWAYS`). For each tax type the most specific matching row wins (municipality outranks regime). ISS is always charged; any tax whose rule says it is withheld for this tomador is also recorded in `withholdings` (ISS included). No rates are hardcoded: a new service/municipality/regime is data only, and the table ships empty (nothing is seeded).
- **ISS rate (AC3).** Defaults to the matching `ISS` rule; `issRateOverride` (0-100) requires a non-blank `overrideJustification` and replaces the rate while keeping the configured ISS withholding behaviour. With no configured rate, a justified override still works; with neither, the RPS is rejected. The justification is stored on the document.
- **Service code validation (AC1).** `ServiceCode` checks the LC 116/2003 structure (item 01-40, subitem 01-99; `1.05`, `0105` and `01.05` normalise to `01.05`); it is a structural check, not an embedded copy of every subitem. The municipal list (`municipal_service_codes`) is then checked for the municipality **ISS is due to** (provider's for `PROVIDER`, tomador's for `RECIPIENT`, per §5.2's "selected municipality", which is what the AC's "tomador's municipality" is when the place of provision is the recipient). A municipality with no list configured falls back to LC 116 alone; one with a list only accepts the codes on it.
- **Full address (AC2).** Required (street, number, neighborhood, zip code, state **and** municipality IBGE code; complement optional) whenever at least one tax ends up withheld. A PF tomador with no withholdings needs none. `placeOfProvision = RECIPIENT` additionally requires the tomador's municipality.
- Not done here: `DiscriminationTemplate` pre-fill (M4-06; `discrimination` is plain text for now) and cross-checking IBGE codes against the `masterdata` IBGE table.
