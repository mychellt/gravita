# UC-M10-12 — Trigger Backup (`TriggerBackupUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 8 — Robustez (doc §14)

## Functional requirement

§11.4 — "Backup automático: Banco de dados e XMLs fiscais com backup diário; retenção de 90 dias mínimo."

## Description

Runs (on a daily schedule, or on demand via the endpoint) a backup of the database and the fiscal XML object store, and records the run. Precondition: none beyond `BackupExecutionPort` being configured. Postcondition: a `BackupJob` record exists with outcome and location; backups older than the 90-day minimum retention may be pruned, never sooner.

## Port signature

```java
public interface TriggerBackupUseCase {
    BackupJobId execute(TriggerBackupCommand command);
}
```

`TriggerBackupCommand`: `scope: {DATABASE, FISCAL_XML, BOTH}` (defaults to `BOTH` on the scheduled daily run). Returns the created `BackupJobId`.

## Outbound ports required

- `BackupExecutionPort`

## REST endpoint

`POST /api/system/backups/run`

## Domain entities touched

- `BackupJob`

## Acceptance criteria

- [ ] A daily scheduled run exists in addition to the on-demand endpoint.
- [ ] Every run (success or failure) is recorded as a `BackupJob`.
- [ ] A failed backup raises a `MonitoringAlert` (UC-M10-13) — silent failures are not acceptable given the 90-day retention requirement.
- [ ] Retained backups are never pruned before 90 days.

## Dependencies

- **Depends on:** None.
- **Blocks:** None — but is a hard prerequisite for going to production, since M2's `XmlObjectStoragePort` (doc §13) has no other durability guarantee without it.
