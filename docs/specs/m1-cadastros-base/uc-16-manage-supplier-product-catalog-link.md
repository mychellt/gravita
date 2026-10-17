# UC-M1-16 — Manage Supplier Product Catalog Link (`UpsertSupplierProductCatalogLinkUseCase`, `FindSupplierProductCatalogLinkUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14), needed by the inbound-NFe conference

## Functional requirement

§2.4 Produtos e Serviços + §3.3 (purchase NFe): a supplier identifies a product by its own code. The system must remember which catalog `Product` that code stands for, per company, so a repeat purchase needs no manual product pick, and must keep the last price paid.

## Description

`SupplierProductCatalogLink` is a master-data link owned alongside `Supplier` and `Product`: `companyId`, `supplierId`, `supplierProductCode`, `productId`, `lastPurchaseUnitValue`, `lastPurchaseAt`. It is unique per (`companyId`, `supplierId`, `supplierProductCode`); a `Product` can be linked from several suppliers at once, each link with its own last price and date. `tax` creates/refreshes links while resolving and confirming an inbound NFe ([UC-M2-15](../m2-fiscal-nfe/uc-15-resolve-inbound-nfe-supplier-and-products.md), [UC-M2-10](../m2-fiscal-nfe/uc-10-confirm-inbound-nfe-receipt.md)).

Rules: the supplier and the product must exist; the stored price is always the **last** price (never an average); a link is never re-pointed to another product (editing/un-linking is out of scope), so upserting an existing (company, supplier, code) with a different product is rejected. Every operation is scoped by `companyId`.

## Port signatures

```java
public interface UpsertSupplierProductCatalogLinkUseCase {
    SupplierProductCatalogLink execute(UpsertSupplierProductCatalogLinkCommand command);
}

public interface FindSupplierProductCatalogLinkUseCase {
    Optional<SupplierProductCatalogLink> bySupplierProductCode(CompanyId companyId, SupplierId supplierId, String supplierProductCode);
    List<SupplierProductCatalogLink> byProduct(CompanyId companyId, UUID productId);   // a product's linked suppliers
}
```

`UpsertSupplierProductCatalogLinkCommand`: `companyId`, `supplierId`, `supplierProductCode`, `productId`, `unitValue`, `purchasedAt`.

## Outbound ports required

- `SupplierProductCatalogLinkRepositoryPort` (company-scoped; `findBySupplierAndCode`, `findBySupplierAndCodes`, `findAllByProduct`, `save`)
- `SupplierRepositoryPort` (`findById`, and `findByDocument` used by `tax` to resolve the CNPJ), `ProductRepositoryPort`

## Persistence

Table `supplier_product_catalog_links` (migration `V97` in `db/migration/{local,hom,prod}`), unique `(company_id, supplier_id, supplier_product_code)`, index `(company_id, product_id)`.

## REST endpoint

None yet: the capability is consumed by the use cases above. Listing a product's linked suppliers in the UI is a later ticket.

## Domain entities touched

- `SupplierProductCatalogLink` (created/updated), `Supplier`, `Product` (read)

## Acceptance criteria

- [ ] Creates the link seeded with the given price and date when none exists for (company, supplier, code); refreshes price/date of an existing link to the same product, keeping its id.
- [ ] Rejects a link to a supplier or product that does not exist, and never re-points an existing link to another product.
- [ ] A Product can carry links from more than one supplier at once, each with its own last-purchase price/date (AC7 of UC-M2-15).
- [ ] Lookups by (supplierId, code) and by productId are scoped per `companyId`: the same supplier+code in another company never resolves (AC9 of UC-M2-15).

## Dependencies

- **Depends on:** [UC-M1-09](uc-09-register-supplier.md), [UC-M1-11](uc-11-register-product.md).
- **Blocks:** [UC-M2-15](../m2-fiscal-nfe/uc-15-resolve-inbound-nfe-supplier-and-products.md), [UC-M2-10](../m2-fiscal-nfe/uc-10-confirm-inbound-nfe-receipt.md).
