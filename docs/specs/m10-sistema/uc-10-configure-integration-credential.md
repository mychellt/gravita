# UC-M10-10 — Configure Integration Credential (`ConfigureIntegrationCredentialUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§11.3 — Registers/rotates the endpoint and credentials for each external integration: SEFAZ, Receita Federal, ViaCEP/IBGE, banks, WhatsApp Business API, e-commerce platforms, accounting export.

## Description

An administrator registers or rotates the endpoint/credentials for one named external integration, optionally scoped to an environment (production/homologation, relevant for SEFAZ). Precondition: requester holds `system → integrations → edit`. Postcondition: the stored `IntegrationCredential` is what every adapter implementation (SEFAZ client, Receita Federal client, bank clients, WhatsApp Business API client, e-commerce webhooks, accounting export) reads at call time.

## Port signature

```java
public interface ConfigureIntegrationCredentialUseCase {
    void execute(ConfigureIntegrationCredentialCommand command);
}
```

`ConfigureIntegrationCredentialCommand`: `integrationName`, `environment` (optional — `PRODUCTION`/`HOMOLOGATION`), `endpoint`, `credentialPayload` (opaque, encrypted at rest per doc §11.4).

## Outbound ports required

- `IntegrationCredentialRepositoryPort`

## REST endpoint

`PUT /api/system/integrations/{name}/credentials`

## Domain entities touched

- `IntegrationCredential`

## Acceptance criteria

- [ ] Credential payload is encrypted at rest (doc §11.4).
- [ ] SEFAZ credentials are stored per environment (production and homologation independently), matching M1's per-company `SefazEnvironment` switch.
- [ ] Rotating a credential doesn't require redeploying the adapter that consumes it.
- [ ] Unknown `integrationName` is rejected — only the seven integrations listed in §11.3 are valid targets.

## Dependencies

- **Depends on:** None.
- **Blocks:** Every integration-consuming use case across modules — M2/M3/M4's SEFAZ submission, M1's CNPJ/CEP lookups, M8's bank integration, M3/M7/M2's WhatsApp delivery, M7's e-commerce import, M2's accounting export — none can run without a credential configured here first.
