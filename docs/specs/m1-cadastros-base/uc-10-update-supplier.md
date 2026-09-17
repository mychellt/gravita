# UC-M1-10 — Update Supplier (`UpdateSupplierUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.3 Fornecedores: same field set as registration, editable after creation, with purchase-order/return/quality history preserved.

## Description

A user edits an existing supplier's data. Precondition: the supplier exists (UC-09). Postcondition: the supplier's fields are updated; its history (purchase orders, returns, quality occurrences — sourced from `purchasing`) is untouched.

## Port signature

```java
public interface UpdateSupplierUseCase {
    void execute(UpdateSupplierCommand command);
}
```

`UpdateSupplierCommand`: `supplierId`, plus any subset of the fields from `RegisterSupplierCommand` (partial update).

## Outbound ports required

- `SupplierRepositoryPort`

## REST endpoint

`PATCH /api/suppliers/{id}`

## Domain entities touched

- `Person` (embedded)
- `Supplier`

## Acceptance criteria

- [ ] Partial updates don't require re-submitting unrelated fields.
- [ ] Changing `defaultPurchaseCfop` or `pixKey` doesn't retroactively alter already-issued purchase orders.
- [ ] Editing a supplier never deletes or rewrites its purchase/return/quality history.

## Dependencies

- **Depends on:** UC-09 (Register Supplier).
- **Blocks:** None beyond keeping downstream `purchasing` (M6) data current.
