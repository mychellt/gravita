# UC-M10-13 — Record Monitoring Alert (`RecordMonitoringAlertUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 8 — Robustez (doc §14)

## Functional requirement

§11.4 — "Monitoramento: Alertas automáticos para falhas de transmissão, jobs atrasados e erros críticos." / §11.3 SEFAZ availability monitoring feeds the same alert channel.

## Description

Records a monitoring alert (transmission failure, delayed background job, critical error, or a SEFAZ availability transition) and notifies technical support. Any context can raise one through `AlertNotificationPort`'s write side; this use case is the read/query side plus the persistence of the alert record itself. Precondition: none. Postcondition: an unresolved `MonitoringAlert` is visible to technical support until acknowledged.

## Port signature

```java
public interface RecordMonitoringAlertUseCase {
    Page<MonitoringAlert> execute(GetMonitoringAlertsQuery query);
}
```

`GetMonitoringAlertsQuery`: optional `severity`, `source`, `resolved`, plus pagination. (The write path — raising a new alert — is invoked internally by other use cases, e.g. UC-M10-11 and M2/M3/M4's transmission-queue worker, not exposed as a separate command in this ticket.)

## Outbound ports required

- `AlertNotificationPort`

## REST endpoint

`GET /api/system/alerts`

## Domain entities touched

- `MonitoringAlert`

## Acceptance criteria

- [ ] Transmission failures from `tax`'s queue worker, delayed background jobs, and critical errors all land in the same `MonitoringAlert` list.
- [ ] Alerts are filterable by resolved/unresolved status.
- [ ] Technical support can see the alert without needing access to the module that raised it (M10 is the single aggregation point, per doc §11.4).

## Dependencies

- **Depends on:** None to read; the write side depends on whichever use case raises the alert (e.g. UC-M10-11, M2/M3/M4's transmission queue, UC-M10-12 on backup failure).
- **Blocks:** None.
