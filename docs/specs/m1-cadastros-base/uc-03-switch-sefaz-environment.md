# UC-M1-03 — Switch SEFAZ Environment (`SwitchSefazEnvironmentUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.1 Empresa / Filiais: SEFAZ environment (Production / Homologation) set per company, switchable without restarting the system.

## Description

An administrator toggles a company between `PRODUCTION` and `HOMOLOGATION` SEFAZ environments. The switch takes effect immediately for the next fiscal-document transmission — no application restart or deploy is required. Precondition: the company exists. Postcondition: subsequent M2/M3/M4 transmissions for that company target the new environment.

## Port signature

```java
public interface SwitchSefazEnvironmentUseCase {
    void execute(SwitchSefazEnvironmentCommand command);
}
```

`SwitchSefazEnvironmentCommand`: `companyId`, `environment: {PRODUCTION, HOMOLOGATION}`.

## Outbound ports required

- `CompanyRepositoryPort`

## REST endpoint

`PATCH /api/companies/{id}/sefaz-environment`

## Domain entities touched

- `Company` (`sefazEnvironment` field)

## Acceptance criteria

- [ ] The switch is effective immediately for the same company, with no restart.
- [ ] The environment is scoped per company, not global (a multi-company tenant can have one company in homologation and another in production).
- [ ] `M2`/`M3`/`M4`'s `SubmitToSefazPort` reads this field at transmission time rather than caching it.

## Dependencies

- **Depends on:** UC-01 (Register Company).
- **Blocks:** None directly, but M2/M3/M4 transmission behavior depends on this field being correct.
