# M4 — Fiscal: NFSe

Source: ERP MVP doc §5. Context package: `br.gravita.tax` (adapter: NFSe).

## Purpose

NFSe is the most heterogeneous fiscal module: every municipality runs its own system and communication standard. The MVP strategy is to cover the standards that represent most municipalities, and stay extensible for the rest.

## Functional requirements

### 5.1 Cobertura de Padrões (Standards Coverage)

- ABRASF 2.x: the most adopted standard; covers most large Brazilian municipalities.
- NFS-e Nacional SEFAZ: federal standard launched in 2023; growing adoption among smaller municipalities.
- ISS.net / Betha: proprietary standards from common providers, integrated via a dedicated adapter.
- Municipality registration: each municipality records its webservice URL, standard, version, required certificate and required fields.
- Extensibility: new standards are added as adapters, without changing the module's core.

### 5.2 Emissão

- RPS: Recibo Provisório de Serviços issued internally; converted in batch or individually.
- Service code: LC 116/2003 list plus the municipal list; auto-validated for the selected municipality.
- Place of provision: provider or recipient — determines which municipality ISS is due to.
- Tomador (recipient): PF or PJ; full address is required for source withholding.
- ISS rate: configurable per service and municipality; manual override requires a justification.
- Withholdings: ISS, PIS, COFINS, CSLL, IRPJ, INSS — activated by service type and recipient profile.
- Discrimination: a rich free-text field, with a configurable template per service type to speed up issuance.
- Series/numbering: independent from NFe, controlled per company and municipality.
- Status: Draft → Sent → Authorized → Cancelled, with timestamps and protocols.
- Cancellation: requested via the municipality's webservice; justification required.
- For municipalities without homologated integration, the system generates the XML in the correct standard and guides the user to a manual upload on the municipality's portal — avoiding a total block of the operation.

## Domain model

- **MunicipalityIntegration** (entity, owned by `masterdata`'s `IbgeMunicipality`, configured here) — `standard: {ABRASF, NFSE_NACIONAL, ISSNET, BETHA}`, `version`, `webserviceUrl`, `requiredCertificateType`, `requiredFields`, `homologated: boolean`.
- **NfseDocument** (aggregate root) — `rps{number, series}`, `serviceCode` (LC 116 + municipal list), `placeOfProvision: {PROVIDER, RECIPIENT}`, `provider: CompanyRef`, `tomador: PersonRef`, `issRate: Percentage` (+ override justification), `withholdings: {iss, pis, cofins, csll, irpj, inss}`, `discrimination`, `series`, `number`, `status: {DRAFT, SENT, AUTHORIZED, CANCELLED}`, `protocol`, timestamps. Invariant: `number` is scoped per `(company, municipality)`, independent of the NFe series.
- **DiscriminationTemplate** — per service type, pre-fills the free-text discrimination field.

## Use cases (`application.port.in`)

Each of this module's own use cases has a standalone implementation ticket under [`m4-fiscal-nfse/`](m4-fiscal-nfse/README.md); the shared tax-calculation use case is owned by M2.

| Use case | Responsibility |
|---|---|
| [`RegisterMunicipalityIntegrationUseCase`](m4-fiscal-nfse/uc-01-register-municipality-integration.md) | Configure a municipality's webservice/standard/required fields. |
| [`IssueRpsUseCase`](m4-fiscal-nfse/uc-02-issue-rps.md) | Create an internal RPS. |
| [`ConvertRpsToNfseUseCase`](m4-fiscal-nfse/uc-03-convert-rps-to-nfse.md) | Batch or individual conversion to `NfseDocument`. |
| [`CalculateTaxUseCase`](m2-fiscal-nfe/uc-02-calculate-tax.md) (shared, M2) | ISS rate resolution and withholding computation. |
| [`TransmitNfseUseCase`](m4-fiscal-nfse/uc-04-transmit-nfse.md) | Submit via the standard-specific adapter (`IssueNfsePort`); falls back to guided manual upload when the municipality isn't homologated. |
| [`CancelNfseUseCase`](m4-fiscal-nfse/uc-05-cancel-nfse.md) | Cancellation via municipality webservice, with justification. |
| [`ManageDiscriminationTemplateUseCase`](m4-fiscal-nfse/uc-06-manage-discrimination-template.md) | CRUD for per-service-type templates. |

## Outbound ports (`application.port.out`)

| Port | Purpose |
|---|---|
| `NfseRepositoryPort`, `MunicipalityIntegrationRepositoryPort` | Persistence. |
| `IssueNfsePort` | Single interface, one implementation per municipal standard: `AbrasfIssueNfseAdapter`, `NfseNacionalIssueNfseAdapter`, `IssNetIssueNfseAdapter`, `BethaIssueNfseAdapter`. Adding a municipality means adding an adapter (doc §13). |
| `GenerateGuidedManualUploadPort` | Produces the standard-correct XML plus instructions when no homologated adapter exists. |
| `XmlObjectStoragePort` (shared with M2) | XML storage. |

## Adapters

### Inbound (`adapter.in.web`)

| Method & path | Use case |
|---|---|
| `POST /api/municipalities/{ibgeCode}/integration` | `RegisterMunicipalityIntegrationUseCase` |
| `POST /api/nfse/rps` | `IssueRpsUseCase` |
| `POST /api/nfse/rps/convert` | `ConvertRpsToNfseUseCase` (batch or single) |
| `POST /api/nfse/{id}/cancel` | `CancelNfseUseCase` |
| `GET/POST /api/nfse/discrimination-templates` | `ManageDiscriminationTemplateUseCase` |

### Outbound (`adapter.out.persistence` / integrations)

`NfseJpaEntity`, `RpsJpaEntity`, `MunicipalityIntegrationJpaEntity`; one `IssueNfsePort` implementation per standard, each isolated behind its own package (e.g. `adapter.out.nfse.abrasf`, `adapter.out.nfse.nfsenacional`, `adapter.out.nfse.issnet`) so a new municipality never touches the domain/application layers.

## Cross-module dependencies

- **Consumes from `masterdata`**: company fiscal data, certificate, IBGE municipality table, customer/supplier as tomador.
- **Shares with `M2`/`M3`**: `CalculateTaxUseCase`.
- **Provides to `finance`**: accounts receivable on authorized NFSe (service sales), same pattern as M7's invoicing trigger.

## Notes

- The doc doesn't enumerate every ABRASF/NFS-e Nacional/ISS.net field; those are protocol-specific XML schemas to be modeled inside each adapter package, not in the shared `NfseDocument` aggregate.
- "Guided manual upload" is described functionally (generate correct XML, instruct the user) but the doc doesn't specify a UI for it; it's treated here as a first-class outcome of `TransmitNfseUseCase`, not an error state.
