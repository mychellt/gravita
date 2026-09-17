# UC-M1-11 — Register Product (`RegisterProductUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.4 Produtos e Serviços: identification (internal code, barcodes), type (simple/variant/kit/service), fiscal data (NCM, CEST, origin, CFOP, CST/CSOSN), tax profile, prices, stock parameters, traceability flags, classification, images (max 5), status.

## Description

A user registers a new product, service or kit, including its tax profile — the data every other module (`tax`, `inventory`, `purchasing`, `sales`) reads. Precondition: none beyond the company context. Postcondition: the product exists with `status: ACTIVE` by default and can be referenced by price tables, stock movements, purchase and sales items.

## Port signature

```java
public interface RegisterProductUseCase {
    ProductId execute(RegisterProductCommand command);
}
```

`RegisterProductCommand`: `internalCode`, `barcodes`, `type: {SIMPLE, VARIANT, KIT, SERVICE}`, `ncm`, `cest`, `origin (0–8)`, `defaultCfopByOperation`, `cstCsosnByState`, `taxProfile`, `averageCost`, `basePrice`, `stock{min,max,reorderPoint}`, `purchaseUnit`, `saleUnit`, `conversionFactor`, `lotControl`, `serialControl`, `classification{group,subgroup,brand,section}`, `images (max 5)`, and for `KIT` type, the composed product list.

## Outbound ports required

- `ProductRepositoryPort`

## REST endpoint

`POST /api/products`

## Domain entities touched

- `Product`

## Acceptance criteria

- [ ] `type = SERVICE` products carry no stock fields (min/max/reorder/units) — the domain model explicitly excludes them.
- [ ] `type = KIT` composes other `Product`s; the composition must reference existing products.
- [ ] `type = VARIANT` supports a color/size grid without requiring separate `Product` records per combination, per doc §2.4.
- [ ] At most 5 images are accepted; a 6th is rejected with a clear message.
- [ ] `status` initializes to `ACTIVE`.
- [ ] Barcode uniqueness (EAN-13/DUN-14) is enforced across the product catalog.

## Dependencies

- **Depends on:** None.
- **Blocks:** UC-12 (Update Product), UC-13 (Manage Price Table) entries, and every `inventory` (M5), `purchasing` (M6), `sales` (M7) and `tax` (M2/M3/M4) use case that references a product.
