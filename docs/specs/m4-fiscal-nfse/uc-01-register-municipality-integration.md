# UC-M4-01 — Register Municipality Integration (`RegisterMunicipalityIntegrationUseCase`)

**Module:** M4 — Fiscal: NFSe ([module spec](../m4-fiscal-nfse.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 6 — Serviços (doc §14)

## Functional requirement

Municipality registration: each municipality records its webservice URL, standard, version, required certificate and required fields (§5.1). New standards are added as adapters, without changing the module's core (§5.1).

## Description

Admin registers or updates the NFSe integration configuration for a municipality (identified by IBGE code): which standard it speaks (ABRASF, NFS-e Nacional, ISS.net, Betha), the webservice version, the certificate type it requires, and any municipality-specific required fields. This configuration is what `TransmitNfseUseCase` reads to pick the right `IssueNfsePort` adapter, and whether that adapter is homologated.

## Port signature

```java
public interface RegisterMunicipalityIntegrationUseCase {
    MunicipalityIntegrationId execute(RegisterMunicipalityIntegrationCommand command);
}
```

`RegisterMunicipalityIntegrationCommand`: `ibgeCode`, `standard` (`ABRASF` | `NFSE_NACIONAL` | `ISSNET` | `BETHA`), `version`, `webserviceUrl`, `requiredCertificateType`, `requiredFields`, `homologated`. Returns the created/updated `MunicipalityIntegration` id.

## Outbound ports required

- `MunicipalityIntegrationRepositoryPort`

## REST endpoint

`POST /api/municipalities/{ibgeCode}/integration`

## Domain entities touched

- `MunicipalityIntegration`

## Acceptance criteria

- [ ] A municipality can be registered with one of the four supported standards.
- [ ] `homologated: false` is a valid, supported state — it doesn't block registration, only routes `TransmitNfseUseCase` to the guided manual-upload path.
- [ ] Re-registering the same `ibgeCode` updates the existing configuration rather than creating a duplicate.
- [ ] Registering a municipality with a standard for which no `IssueNfsePort` adapter is deployed does not fail at registration time (the adapter gap only surfaces at transmission time).

## Dependencies

- **Depends on:** None.
- **Blocks:** UC-M4-04 (`transmit-nfse`), which reads this configuration to select the adapter.
