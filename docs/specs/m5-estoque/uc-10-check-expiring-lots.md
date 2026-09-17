# UC-M5-10 — Check Expiring Lots (`CheckExpiringLotsUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Validade: Alerta de lotes a vencer em X dias (configurável); nunca vende lote vencido." (§6.2)

## Description

A scheduled query that scans `Lot`s with an `expiryDate` within a configurable X-day window and surfaces them as alerts. Feeds the M9 dashboard's "estoque crítico" widget and M10's alerting/notification pipeline. Read-only — it never blocks a sale itself; blocking expired-lot allocation is enforced by [UC-M5-03](uc-03-register-stock-exit.md).

## Port signature

```java
public interface CheckExpiringLotsUseCase {
    List<ExpiringLotView> execute(CheckExpiringLotsQuery query);
}
```

`CheckExpiringLotsQuery`: `withinDays` (configurable threshold), `warehouseId` (optional filter). Returns a list of `ExpiringLotView` (product, lot code, expiry date, remaining quantity).

## Outbound ports required

- `LotRepositoryPort`
- `NotifyExpiringLotPort`

## REST endpoint

`GET /api/inventory/alerts/expiring-lots`

## Domain entities touched

- `Lot` (read-only)

## Acceptance criteria

- [ ] Only lots with `expiryDate <= today + withinDays` and remaining quantity `> 0` are returned.
- [ ] `withinDays` is configurable, not hardcoded, per the functional requirement.
- [ ] Results are pushed through `NotifyExpiringLotPort` so M9/M10 can surface them without polling this endpoint directly.

## Dependencies

- **Depends on:** [UC-M5-02](uc-02-register-stock-entry.md) (lots must exist to be checked).
- **Blocks:** M9's dashboard "estoque crítico" widget; M10's alerting pipeline.
