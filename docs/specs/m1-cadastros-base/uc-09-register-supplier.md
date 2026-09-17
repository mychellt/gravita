# UC-M1-09 — Register Supplier (`RegisterSupplierUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.3 Fornecedores: same PF/PJ structure as customers, reusing the same form; bank data (checking account, agency, bank, PIX key); operation data (average lead time, default purchase CFOP).

## Description

A user registers a new supplier, reusing the same `Person` structure and (optionally) the same UC-07 lookup used for customers. Precondition: none beyond the company context. Postcondition: the supplier exists and can be referenced by purchase requests/orders (M6).

## Port signature

```java
public interface RegisterSupplierUseCase {
    SupplierId execute(RegisterSupplierCommand command);
}
```

`RegisterSupplierCommand`: `type: PersonType {PF, PJ}`, `document: Document`, name/company name, addresses, contacts, `bankAccount`, `pixKey`, `averageLeadTimeDays`, `defaultPurchaseCfop`.

## Outbound ports required

- `SupplierRepositoryPort`

## REST endpoint

`POST /api/suppliers`

## Domain entities touched

- `Person` (embedded)
- `Supplier`

## Acceptance criteria

- [ ] The PF/PJ form and validation logic are shared with `RegisterCustomerUseCase` (UC-05) rather than duplicated (doc: "reusing the same form").
- [ ] `pixKey` accepts CPF, CNPJ, e-mail or a random key format.
- [ ] `averageLeadTimeDays` and `defaultPurchaseCfop` are optional at creation but required before the supplier can be used in a purchase order (M6).

## Dependencies

- **Depends on:** None directly; commonly preceded by UC-07 (Lookup Person by Document).
- **Blocks:** UC-10 (Update Supplier) and every `purchasing` (M6) use case that references a supplier.
