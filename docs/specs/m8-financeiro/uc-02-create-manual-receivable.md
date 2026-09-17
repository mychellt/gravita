# UC-M8-02 — Create Manual Receivable (`CreateManualReceivableUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.1 Contas a Receber — "Origem: ... lançamento manual para cobranças avulsas."

## Description

User-initiated creation of a one-off receivable not tied to an invoice (e.g. a standalone charge). Actor: financial user. Precondition: the customer is already registered in `masterdata`. Creates a `Receivable` with `origin = MANUAL`, `status = OPEN`.

## Port signature

```java
public interface CreateManualReceivableUseCase {
    Receivable execute(CreateManualReceivableCommand command);
}
```

`CreateManualReceivableCommand`: `customer` (ref), `amount: Money`, `dueDate`, `installments` (optional). Returns the created `Receivable`.

## Outbound ports required

- `ReceivableRepositoryPort`

## REST endpoint

`POST /api/finance/receivables`

## Domain entities touched

- `Receivable`

## Acceptance criteria

- [ ] A `Receivable` is created with `origin = MANUAL`.
- [ ] `amount` and `dueDate` are required; `customer` must resolve to a registered `masterdata` customer.
- [ ] The created title starts in `OPEN` status.

## Dependencies

- **Depends on:** None.
- **Blocks:** UC-M8-03, UC-M8-04, UC-M8-06, UC-M8-07, UC-M8-08, UC-M8-09.
