# UC-M1-12 — Update Product (`UpdateProductUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.4 Produtos e Serviços: same field set as registration, editable after creation; setting `status: OUT_OF_STOCK` or `INACTIVE` hides the product from sale while keeping its history.

## Description

A user edits an existing product's data, including deactivating it. Precondition: the product exists (UC-11). Postcondition: the product's fields are updated; if `status` moves to `INACTIVE`/`OUT_OF_STOCK`, the product stops appearing in sale/quote screens but its stock and sales history are preserved.

## Port signature

```java
public interface UpdateProductUseCase {
    void execute(UpdateProductCommand command);
}
```

`UpdateProductCommand`: `productId`, plus any subset of the fields from `RegisterProductCommand` (partial update), including `status`.

## Outbound ports required

- `ProductRepositoryPort`

## REST endpoint

`PATCH /api/products/{id}`

## Domain entities touched

- `Product`

## Acceptance criteria

- [ ] Partial updates don't require re-submitting unrelated fields.
- [ ] Setting `status = INACTIVE` or `OUT_OF_STOCK` immediately hides the product from `sales`/PDV search but not from historical records or reports.
- [ ] Changing tax-profile fields (NCM, CFOP, CST/CSOSN) doesn't retroactively alter already-issued fiscal documents.
- [ ] Changing `lotControl`/`serialControl` from enabled to disabled is rejected while the product still has open lots/serials in `inventory` (M5).

## Dependencies

- **Depends on:** UC-11 (Register Product).
- **Blocks:** None beyond keeping downstream `inventory`/`sales`/`tax` data current.
