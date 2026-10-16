# UC-M2-15 — Resolve Inbound NFe Supplier/Item (`GetInboundNfeForConferenceUseCase`, `ResolveInboundNfeItemAsNewProductUseCase`, `ResolveInboundNfeItemAsExistingProductUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14), GRA-246

## Functional requirement

GRA-245/GRA-246: an imported or manually-entered inbound NF-e (UC-08/UC-09) only carries free-text supplier/item data from the document itself; before conference ([UC-10](uc-10-confirm-inbound-nfe-receipt.md)) can be confirmed, its supplier must be matched to a registered `Supplier` and each of its items to a catalog `Product`, without ever rewriting the originally imported `InboundNfe`/`InboundNfeItem` (BR8).

## Description

Three related use cases, all computed dynamically — nothing from them is persisted on `InboundNfe` itself:

- **`GetInboundNfeForConferenceUseCase`** powers the conference screen: resolves the supplier by CNPJ (`SupplierRepositoryPort.findByDocument`) and, for each item, looks up a [`SupplierProductCatalogLink`](../m1-cadastros-base/uc-16-supplier-product-catalog-link.md) by `(companyId, supplierId, item.supplierProductCode)`. An item with a link pre-fills its product automatically (BR3); one without is flagged unresolved for the operator to act on.
- **`ResolveInboundNfeItemAsNewProductUseCase`** (BR4): for an unresolved item, registers a brand-new `Product` (reusing [`RegisterProductUseCase`](../m1-cadastros-base/uc-11-register-product.md) — every fiscal-output and pricing field still operator-completed, per UC-M1-11's own acceptance criteria) pre-filled from the item's `description`/`ncm`/`unit`, then creates the link seeded with this item's `unitValue`.
- **`ResolveInboundNfeItemAsExistingProductUseCase`** (BR5): for an unresolved item, links it to a `Product` the operator searched and picked from the catalog instead — no new product created. This is what prevents two suppliers' different codes for the same physical product from becoming two different catalog products.

Both resolve use cases reject an item that is already linked (resolution only happens once per supplier+code; re-linking/un-linking is out of scope for GRA-246) and reject any NFe whose supplier isn't registered yet.

## Port signature

```java
public interface GetInboundNfeForConferenceUseCase {
    InboundNfeConferenceView execute(UUID inboundNfeId);
}

public interface ResolveInboundNfeItemAsNewProductUseCase {
    ProductDomain execute(ResolveInboundNfeItemAsNewProductCommand command); // inboundNfeId, itemIndex, productToRegister
}

public interface ResolveInboundNfeItemAsExistingProductUseCase {
    SupplierProductCatalogLink execute(ResolveInboundNfeItemAsExistingProductCommand command); // inboundNfeId, itemIndex, productId
}
```

## Outbound ports required

- `InboundNfeRepositoryPort`, `SupplierRepositoryPort`, `SupplierProductCatalogLinkRepositoryPort`, `ProductRepositoryPort`
- `RegisterProductPort` (reused, not re-implemented, for the new-product path)
- `UpsertSupplierProductCatalogLinkUseCase` (UC-M1-16)

## REST endpoints

- `GET /api/nfe/inbound/{id}/conference`
- `POST /api/nfe/inbound/{id}/items/{itemIndex}/link-new-product`
- `POST /api/nfe/inbound/{id}/items/{itemIndex}/link-existing-product`

## Domain entities touched

- None mutated — only reads `InboundNfe`/`InboundNfeItem`, and creates/updates `SupplierProductCatalogLink` and (new-product path) `Product`.

## Acceptance criteria

(Numbered to match GRA-246's AC1-AC10.)

- [ ] AC1: importing an NF-e whose `supplierDocument` CNPJ matches an existing `Supplier` marks it supplier-resolved in the import response; no match leaves it unresolved without blocking the import.
- [ ] AC4: when a link exists for `(supplierId, item.supplierProductCode)`, the conference view pre-fills that item's product with no manual search.
- [ ] AC5: registering a new product from an unresolved item pre-fills description/NCM/unit, still requires every UC-M1-11 fiscal-output/pricing field, and creates both the product and its link on save.
- [ ] AC6: linking an unresolved item to an existing product creates the link without creating a new product.
- [ ] AC7: a product can carry links from more than one supplier, each with its own last-purchase price/date.
- [ ] AC9: every lookup/creation here is scoped by `companyId` - the same CNPJ+code in a different company never resolves to another company's product.
- [ ] AC10: none of the three use cases ever mutates the originally imported `InboundNfeItem`.

## Dependencies

- **Depends on:** [UC-08](uc-08-import-supplier-nfe-xml.md)/[UC-09](uc-09-enter-inbound-nfe-manually.md) (an `InboundNfe` must exist); UC-M1-09 (Register Supplier); UC-M1-11 (Register Product); UC-M1-16 (the link itself).
- **Blocks:** [UC-10](uc-10-confirm-inbound-nfe-receipt.md), which rejects confirmation (AC2/AC3) while the supplier or any item is still unresolved, and applies the BR7 price-variance update to each item's link on success.
