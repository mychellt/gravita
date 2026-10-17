# UC-M2-15 — Resolve Inbound NFe Supplier and Products (`ResolveInboundNfeItemUseCase`, `GetInboundNfeConferenceUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax` (reads/writes `br.gravita.masterdata` through [UC-M1-16](../m1-cadastros-base/uc-16-manage-supplier-product-catalog-link.md))
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.3 (XML import, conference, automatic stock entry): the operator should never re-pick the catalog product of an item they already bought from the same supplier, and two suppliers selling the same physical product under different codes/descriptions must not end up as two catalog products by accident.

## Description

An imported/typed `InboundNfe` only carries a snapshot of what the supplier's XML said (free-text supplier, item code, description, NCM, unit). This ticket resolves that snapshot against the registries **on demand**, without persisting anything on the `InboundNfe`:

- **Supplier (BR1)** — the `Supplier` whose document equals the NFe's `supplierDocument` (CNPJ), looked up through `SupplierRepositoryPort`. A supplier registered after the import is picked up on the next look-up; no backfill. An unmatched supplier never blocks the import ([UC-M2-08](uc-08-import-supplier-nfe-xml.md)).
- **Items (BR2–BR3)** — an item is *resolved* when a `SupplierProductCatalogLink` exists for (the NFe's company, resolved supplier, `item.supplierProductCode`). A resolved item pre-fills its catalog product at conference with no search.
- **Resolve by new product (BR4)** — for an unresolved item the operator registers a new `Product` pre-filled from the item's `description` (→ `internalCode`, the catalog's searchable description), `ncm` and `unit` (→ `purchaseUnit`), reusing [UC-M1-11](../m1-cadastros-base/uc-11-register-product.md) unchanged: every fiscal-output field (sale CFOP, CST/CSOSN by state, tax profile) and pricing field stays operator-completed. Saving registers the product **and** creates the link, seeded with the item's `unitValue`, in one transaction.
- **Resolve by linking an existing product (BR5)** — the operator searches the catalog (`ProductPickerComponent`, `GET /api/pdv/products/search`), picks a product and links it in two actions; no `Product` is created. This is what prevents the duplicate-product scenario across suppliers.
- **Price variance (BR7)** — when a resolved item's `unitValue` differs from the link's `lastPurchaseUnitValue`, the conference shows both values and the delta (non-blocking). On confirmation ([UC-M2-10](uc-10-confirm-inbound-nfe-receipt.md)) the link's last price/date become this NFe's (last price, not an average).
- **Immutability (BR8)** — resolution only creates/updates `SupplierProductCatalogLink`; the imported `InboundNfeItem` snapshot is never mutated.
- Everything is scoped by `companyId`; an inbound NFe of another company looks nonexistent to the caller.

## Port signatures

```java
public interface GetInboundNfeConferenceUseCase {
    InboundNfeConference execute(GetInboundNfeConferenceQuery query);   // (inboundNfeId, callerId)
}

public interface ResolveInboundNfeItemUseCase {
    SupplierProductCatalogLink execute(ResolveInboundNfeItemCommand command);
}
```

`InboundNfeConference`: `inboundNfeId`, `supplierDocument` and `supplierName` (the NF-e's own snapshot, so an unregistered supplier can be registered from the screen), `supplierResolved`, `supplierId`, and per item `index`, `supplierProductCode`, `description`, `ncm`, `unit`, `quantity`, `unitValue`, plus — when linked — `productId`, `productLabel`, `lastPurchaseUnitValue`, `lastPurchaseAt`, `priceDelta`.

`ResolveInboundNfeItemCommand`: `callerId`, `inboundNfeId`, `itemIndex`, and exactly one of `existingProductId` (BR5) or `newProduct` (BR4).

## Outbound ports required

- `InboundNfeRepositoryPort`, `SupplierRepositoryPort` (`findByDocument`), `SupplierProductCatalogLinkRepositoryPort`, `ProductRepositoryPort`
- `RegisterProductPort` ([UC-M1-11](../m1-cadastros-base/uc-11-register-product.md)) and `UpsertSupplierProductCatalogLinkUseCase` ([UC-M1-16](../m1-cadastros-base/uc-16-manage-supplier-product-catalog-link.md))

## REST endpoints

| Endpoint | Purpose |
|---|---|
| `GET /api/nfe/inbound/{id}/conference` | Items with supplier/product resolution and price comparison, for the conference screen. |
| `POST /api/nfe/inbound/{id}/items/{itemIndex}/link-product` | Link an existing product (`{productId}`) to the item's supplier code. |
| `POST /api/nfe/inbound/{id}/items/{itemIndex}/register-product` | Register the product (`RegisterProductRequest`) and link it. |

`POST /api/nfe/inbound/{id}/confirm-receipt` ([UC-M2-10](uc-10-confirm-inbound-nfe-receipt.md)) additionally rejects (400, `{message}`) an unresolved supplier, any unresolved item, and a conference line whose `itemRef` is not the product linked to its item.

## Web

`ConfirmReceiptModalComponent` loads the conference, pre-fills linked products, shows the price variance, and offers "Cadastrar produto novo" (`RegisterProductModalComponent`, pre-filled description/NCM/unit; origin, sale CFOP, CST/CSOSN + UF and sale price required) or "Vincular produto selecionado" (`ProductPickerComponent`) per unresolved item. It blocks confirmation until the supplier and every item are resolved, and says why when the button is clicked.

An intro and a status checklist state up front that the supplier and the products can be registered on the same screen. When the supplier is unresolved, "Cadastrar fornecedor" opens `RegisterSupplierModalComponent`: the NF-e's CNPJ is read-only (it is the key the NF-e matches on), the name is pre-filled, and the operator completes one address (CEP, street, neighborhood, city, UF — the XML does not carry it; `GET /api/lookup/cnpj|cep` pre-fill it best-effort). It posts to `POST /api/suppliers` ([UC-M1-09](../m1-cadastros-base/uc-09-register-supplier.md)) and reloads the conference, which unlocks the per-item actions (disabled, with the reason, while the supplier is unresolved).

## Domain entities touched

- `SupplierProductCatalogLink` (created, last price refreshed), `Product` (created in BR4)
- `InboundNfe` / `InboundNfeItem` — read only

## Acceptance criteria

- [ ] **AC1** Importing an NF-e whose `supplierDocument` CNPJ matches an existing `Supplier` marks it supplier-resolved; no match leaves it unresolved without blocking the import itself.
- [ ] **AC2** `ConfirmInboundNfeReceiptUseCase` rejects confirmation with a clear message when the NF-e's supplier is unresolved, independent of the existing item-count check.
- [ ] **AC3** `ConfirmInboundNfeReceiptUseCase` rejects confirmation with a clear message when any item has no resolved Product.
- [ ] **AC4** When a `SupplierProductCatalogLink` exists for (supplierId, item code), the conference screen pre-fills that item's product with no manual search.
- [ ] **AC5** Registering a new Product from an unresolved item pre-fills description/NCM/unit from the NF-e item, still requires the operator to complete fiscal-output and pricing fields per UC-M1-11, and on save creates both the Product and the link.
- [ ] **AC6** Searching and picking an existing Product for an unresolved item creates the link without creating a new Product.
- [ ] **AC7** A Product can carry links from more than one supplier at once, each with its own last-purchase price/date.
- [ ] **AC8** When the current item's `unitValue` differs from the link's stored price, the conference screen shows both values and the delta before confirmation; confirming updates the link.
- [ ] **AC9** Link lookups/creations are scoped per `companyId` — the same CNPJ+code in a different company never resolves to another company's Product.
- [ ] **AC10** Confirming a receipt never mutates the originally imported `InboundNfeItem` values.

## Out of scope

Automatic/fuzzy matching by description or NCM; editing/un-linking an existing link; a supplier-product purchase-history report; purchase-unit ↔ sale-unit conversion beyond `Product.conversionFactor`.

## Dependencies

- **Depends on:** [UC-M2-08](uc-08-import-supplier-nfe-xml.md) / [UC-M2-09](uc-09-enter-inbound-nfe-manually.md) (an `InboundNfe` must exist); [UC-M1-09](../m1-cadastros-base/uc-09-register-supplier.md); [UC-M1-11](../m1-cadastros-base/uc-11-register-product.md); [UC-M1-16](../m1-cadastros-base/uc-16-manage-supplier-product-catalog-link.md).
- **Blocks:** [UC-M2-10](uc-10-confirm-inbound-nfe-receipt.md) (confirmation requires a resolved supplier and items).
