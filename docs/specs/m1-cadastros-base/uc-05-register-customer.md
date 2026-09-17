# UC-M1-05 — Register Customer (`RegisterCustomerUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.2 Clientes: PF/CPF or PJ/CNPJ with real-time check-digit validation; fiscal data (IE indicator, final-consumer flag); multiple independent addresses; credit/risk fields; price-table linkage; multiple contacts.

## Description

A user registers a new customer, either from scratch or pre-filled via UC-07 (`LookupPersonByDocumentUseCase`). Precondition: none beyond the company context. Postcondition: the customer exists with `status: REGULAR` by default and can be referenced by sales orders, price tables and fiscal issuance.

## Port signature

```java
public interface RegisterCustomerUseCase {
    CustomerId execute(RegisterCustomerCommand command);
}
```

`RegisterCustomerCommand`: `type: PersonType {PF, PJ}`, `document: Document`, name/company name, `addresses` (billing/delivery, each with a default flag), `ieIndicator`, `finalConsumer`, `creditLimit`, `priceTableIds` (with priority order), `contacts`.

## Outbound ports required

- `CustomerRepositoryPort`

## REST endpoint

`POST /api/customers`

## Domain entities touched

- `Person` (embedded)
- `Customer`

## Acceptance criteria

- [ ] CPF/CNPJ check digit is validated in real time before submission succeeds (doc §2.2).
- [ ] `ieIndicator` and `finalConsumer` are both required for a PJ customer.
- [ ] At least one address is required; billing and delivery can differ and each set has exactly one default.
- [ ] `status` initializes to `REGULAR`; `currentBalance` initializes to zero.
- [ ] Linked price tables are stored with an explicit priority order (doc §2.2).

## Dependencies

- **Depends on:** None directly; commonly preceded by UC-07 (Lookup Person by Document) for auto-fill, and by UC-13 (Manage Price Table) if tables are linked at creation time.
- **Blocks:** UC-06 (Update Customer), UC-08 (Set Customer Credit Status), and every `sales` (M7) and fiscal-issuance (M2/M3/M4) use case that references a customer.
