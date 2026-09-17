# UC-M1-06 — Update Customer (`UpdateCustomerUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.2 Clientes: same field set as registration (addresses, fiscal data, credit/risk, price tables, contacts), editable after creation, with the purchase/payment/return/CRM history preserved on a timeline.

## Description

A user edits an existing customer's data. Precondition: the customer exists (UC-05). Postcondition: the customer's fields are updated; its history timeline (purchases, payments, returns, CRM interactions — sourced from `sales`, `finance`) is untouched by this use case.

## Port signature

```java
public interface UpdateCustomerUseCase {
    void execute(UpdateCustomerCommand command);
}
```

`UpdateCustomerCommand`: `customerId`, plus any subset of the fields from `RegisterCustomerCommand` (partial update).

## Outbound ports required

- `CustomerRepositoryPort`

## REST endpoint

`PATCH /api/customers/{id}`

## Domain entities touched

- `Person` (embedded)
- `Customer`

## Acceptance criteria

- [ ] Partial updates don't require re-submitting unrelated fields.
- [ ] CPF/CNPJ re-validation applies if the document field is changed.
- [ ] `status`, `currentBalance` and price-table links are editable here but are the fields most commonly overwritten by UC-08 and UC-13 respectively — this use case must not silently clobber concurrent writes from those (use optimistic locking or field-level merge).
- [ ] Editing a customer never deletes or rewrites its history timeline.

## Dependencies

- **Depends on:** UC-05 (Register Customer).
- **Blocks:** None beyond keeping downstream `sales`/`finance` data current.
