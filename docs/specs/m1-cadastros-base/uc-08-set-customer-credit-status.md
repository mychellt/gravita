# UC-M1-08 — Set Customer Credit Status (`SetCustomerCreditStatusUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.2 Clientes: credit and risk — credit limit, current balance, status (regular / blocked / delinquent). Per the module spec's domain model, `status` derives from `currentBalance` vs. `creditLimit` plus overdue titles fed by `finance` (read-only here).

## Description

`finance` (M8) calls this use case whenever a customer's delinquency position changes (a title becomes overdue, is settled, or is renegotiated), so that `Customer.status` and `currentBalance` stay current without `finance` writing directly into the `masterdata` schema. Precondition: the customer exists (UC-05). Postcondition: `Customer.status` and `currentBalance` reflect the latest financial position.

## Port signature

```java
public interface SetCustomerCreditStatusUseCase {
    void execute(SetCustomerCreditStatusCommand command);
}
```

`SetCustomerCreditStatusCommand`: `customerId`, `currentBalance: Money`, `status: {REGULAR, BLOCKED, DELINQUENT}`.

## Outbound ports required

- `CustomerRepositoryPort`

## REST endpoint

None — this is an internal inbound port called directly by the `finance` context (M8), not exposed over HTTP. It is not listed in the module spec's REST adapter table.

## Domain entities touched

- `Customer` (`status`, `currentBalance`)

## Acceptance criteria

- [ ] Only `finance` triggers this use case; it is not user-facing (doc: "invoked by `finance` when delinquency changes").
- [ ] `status` transitions follow `currentBalance` vs. `creditLimit` plus overdue-title state — this use case accepts the computed `status` from `finance` rather than recomputing it, to avoid duplicating the delinquency rule in two contexts.
- [ ] Setting `status = BLOCKED` on a customer must be visible immediately to `sales` order approval (M7), so a blocked customer can't get a new order approved.

## Dependencies

- **Depends on:** UC-05 (Register Customer).
- **Blocks:** None in M1; consumed by `finance` (M8) as part of its settlement/aging flows.
