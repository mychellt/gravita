# UC-M8-10 — Create Manual Payable (`CreateManualPayableUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.2 Contas a Pagar — "Origem: ... lançamento manual para despesas."

## Description

User-initiated creation of a one-off payable not tied to a purchase receipt (e.g. rent, utilities). Creates a `Payable` with `origin = MANUAL`, `status = OPEN`.

## Port signature

```java
public interface CreateManualPayableUseCase {
    Payable execute(CreateManualPayableCommand command);
}
```

`CreateManualPayableCommand`: `supplier` (ref, optional for pure expenses), `amount: Money`, `dueDate`, `costCenterSplit` (optional). Returns the created `Payable`.

## Outbound ports required

- `PayableRepositoryPort`

## REST endpoint

`POST /api/finance/payables`

## Domain entities touched

- `Payable`

## Acceptance criteria

- [ ] A `Payable` is created with `origin = MANUAL` and `status = OPEN`.
- [ ] `amount` and `dueDate` are required.

## Dependencies

- **Depends on:** None.
- **Blocks:** UC-M8-12, UC-M8-13, UC-M8-14, UC-M8-15, UC-M8-16.
