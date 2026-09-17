# UC-M10-11 — Monitor SEFAZ Availability (`MonitorSefazAvailabilityUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 8 — Robustez (doc §14)

## Functional requirement

§11.3 — "SEFAZ: NFe, NFCe, NFSe — produção e homologação; monitoramento de disponibilidade."

## Description

A scheduled poller checks each configured SEFAZ endpoint's (production and homologation, per UF) availability status, and exposes the latest known state so `tax` (M2/M3/M4) can decide whether to activate SVC-AN/SVC-RS contingency before a transmission attempt, rather than discovering unavailability only on timeout. Precondition: SEFAZ credentials/endpoints exist (UC-M10-10). Postcondition: the latest status is cached/persisted and queryable.

## Port signature

```java
public interface MonitorSefazAvailabilityUseCase {
    SefazAvailability execute(MonitorSefazAvailabilityQuery query);
}
```

`MonitorSefazAvailabilityQuery`: `uf`, `environment`. `SefazAvailability`: `{status: UP | DOWN | DEGRADED, checkedAt}`.

## Outbound ports required

- `IntegrationCredentialRepositoryPort`
- `AlertNotificationPort` (to raise a `MonitoringAlert` on status change to `DOWN`)

## REST endpoint

`GET /api/system/sefaz-status`

## Domain entities touched

- `IntegrationCredential` (read-only)
- `MonitoringAlert` (written on status transition)

## Acceptance criteria

- [ ] Status is checked per `(uf, environment)` pair independently — one UF's outage doesn't mark others as down.
- [ ] A transition from `UP` to `DOWN`/`DEGRADED` raises a `MonitoringAlert` (UC-M10-13).
- [ ] `tax`'s transmission queue can read the latest status without making its own SEFAZ call.

## Dependencies

- **Depends on:** UC-M10-10 (`ConfigureIntegrationCredentialUseCase`) — SEFAZ endpoints must be configured to poll.
- **Blocks:** None functionally (M2/M3/M4's contingency activation can fall back to per-attempt timeout detection without this), but this ticket is what makes contingency proactive instead of reactive.
