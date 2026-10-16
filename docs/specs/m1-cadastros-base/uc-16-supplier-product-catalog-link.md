# UC-M1-16 — Supplier/Product Catalog Link (`UpsertSupplierProductCatalogLinkUseCase` / `QuerySupplierProductCatalogLinkUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14), driven by GRA-246

## Functional requirement

GRA-245/GRA-246: remember which catalog `Product` a supplier's own item code refers to, so a repeat purchase on a later inbound NF-e resolves automatically instead of the operator re-picking the product every time, and so two suppliers selling the same physical product under different codes don't end up as two different catalog products.

## Description

`tax` (M2) is the only caller today, from inbound NF-e conference ([UC-M2-15](../m2-fiscal-nfe/uc-15-resolve-inbound-nfe-item.md)): a link is created the first time an item is resolved (new product registered, or an existing one picked), and its `lastPurchaseUnitValue`/`lastPurchaseAt` are refreshed on every later confirmed receipt of that same (supplier, code). Precondition: the `Supplier` and `Product` referenced already exist. Postcondition: `(companyId, supplierId, supplierProductCode)` resolves to exactly one `productId`; an existing link's `productId` is never changed by an upsert — only its last-purchase price/date are, and only by a brand-new successful receipt confirmation.

## Port signature

```java
public interface UpsertSupplierProductCatalogLinkUseCase {
    SupplierProductCatalogLink execute(UpsertSupplierProductCatalogLinkCommand command);
}

public interface QuerySupplierProductCatalogLinkUseCase {
    Optional<SupplierProductCatalogLink> findByCode(UUID companyId, UUID supplierId, String supplierProductCode);
    List<SupplierProductCatalogLink> findByProduct(UUID companyId, UUID productId);
}
```

`UpsertSupplierProductCatalogLinkCommand`: `companyId`, `supplierId`, `supplierProductCode`, `productId`, `unitValue`, `purchasedAt`. Creates the link when none exists yet for the key; otherwise only refreshes `lastPurchaseUnitValue`/`lastPurchaseAt`.

## Outbound ports required

- `SupplierProductCatalogLinkRepositoryPort`

## REST endpoint

None directly — reached through `tax`'s inbound NF-e endpoints (`POST /api/nfe/inbound/{id}/items/{itemIndex}/link-new-product`, `.../link-existing-product`, and the `GET /api/nfe/inbound/{id}/conference` read model). `findByProduct` has no caller yet (reserved for a future per-product "linked suppliers" view) and is not exposed over REST today.

## Domain entities touched

- `SupplierProductCatalogLink`

## Acceptance criteria

- [ ] Unique per `(companyId, supplierId, supplierProductCode)` — enforced at the persistence layer (`uq_supplier_product_catalog_links`), not just in application code.
- [ ] A `Product` can carry links from more than one supplier at once, each with its own `lastPurchaseUnitValue`/`lastPurchaseAt` (AC7 of GRA-246).
- [ ] An upsert on an existing link only ever changes `lastPurchaseUnitValue`/`lastPurchaseAt` — the `productId` a code resolves to is immutable once linked (BR8; editing/un-linking a link is out of scope for GRA-246).
- [ ] `lastPurchaseUnitValue`/`lastPurchaseAt` always reflect the single most recent purchase, never an average.
- [ ] Lookups are scoped by `companyId` — the same `(supplierId, supplierProductCode)` in a different company never resolves to another company's link (AC9 of GRA-246).

## Dependencies

- **Depends on:** UC-09 (Register Supplier), UC-11 (Register Product).
- **Blocks:** [UC-M2-15](../m2-fiscal-nfe/uc-15-resolve-inbound-nfe-item.md) and [UC-M2-10](../m2-fiscal-nfe/uc-10-confirm-inbound-nfe-receipt.md)'s item-resolution check.
