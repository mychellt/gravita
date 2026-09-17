# UC-M10-07 — Get Access Log (`GetAccessLogUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§11.1 — "Log de acesso: Login, logout, IP, dispositivo — mantido por 12 meses."

## Description

An administrator queries login/logout history filtered by user, date range, IP or device. Precondition: requester holds `system → access-log → view`. The write side (one `AccessLog` entry per login/logout event) is part of UC-M10-05; this ticket covers only the read/query use case and its 12-month retention.

## Port signature

```java
public interface GetAccessLogUseCase {
    Page<AccessLog> execute(GetAccessLogQuery query);
}
```

`GetAccessLogQuery`: optional `userId`, `dateFrom`, `dateTo`, `ip`, `device`, plus pagination.

## Outbound ports required

- `AccessLogRepositoryPort`

## REST endpoint

`GET /api/system/access-log`

## Domain entities touched

- `AccessLog` (read-only)

## Acceptance criteria

- [ ] Entries older than 12 months are excluded from query results (retention window from the module spec's domain model).
- [ ] Filtering by `userId` returns only that user's events.
- [ ] Requires `system → access-log → view` permission via UC-M10-06.

## Dependencies

- **Depends on:** UC-M10-05 (`AuthenticateUseCase`) writes the entries this ticket reads.
- **Blocks:** None.
