# UC-M7-13 — Set Salesperson Target (`SetSalespersonTargetUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.2 — Metas de vendedor: monthly value and order-count target. (This ticket covers setting the target; tracking is [UC-14](uc-14-get-target-progress.md).)

## Description

Sets or updates a salesperson's monthly target: a revenue value and an order-count goal. Setting a target for a `(salesperson, month)` pair that already has one replaces it.

## Port signature

```java
public interface SetSalespersonTargetUseCase {
    void execute(SetSalespersonTargetCommand command);
}
```

`SetSalespersonTargetCommand`: `salesperson`, `month`, `valueTarget`, `orderCountTarget`.

## Outbound ports required

- `SalespersonTargetRepositoryPort`

## REST endpoint

`PUT /api/crm/targets/{salesperson}/{month}`

## Domain entities touched

- `SalespersonTarget`

## Acceptance criteria

- [ ] Target persisted for the given `(salesperson, month)`.
- [ ] Setting a target for a month that already has one overwrites the previous values (idempotent `PUT` semantics).
- [ ] `valueTarget` and `orderCountTarget` must both be non-negative.

## Dependencies

- **Depends on:** None.
- **Blocks:** [UC-14 Get Target Progress](uc-14-get-target-progress.md).
