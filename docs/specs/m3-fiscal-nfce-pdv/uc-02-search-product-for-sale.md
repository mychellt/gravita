# UC-M3-02 — Search Product for Sale (`SearchProductForSaleUseCase`)

**Module:** M3 — Fiscal: NFCe + PDV ([module spec](../m3-fiscal-nfce-pdv.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§4.1: "Product search: barcode (scanner or typed), internal code, description — immediate result." Real-time search behavior follows the global UX rule (doc §12.1): 300ms debounce, never requires Enter.

## Description

Triggered by the cashier scanning or typing at the PDV. Looks up the product catalog owned by `masterdata` by barcode, internal code or description, and returns immediate matches (with the applicable price) for adding to the cart in [UC-03](uc-03-register-nfce-sale.md).

## Port signature

```java
public interface SearchProductForSaleUseCase {
    List<ProductSearchResult> execute(SearchProductQuery query);
}
```

`SearchProductQuery`: `searchTerm` (barcode, internal code or free-text description), `companyId`/`branchId`, `customerId` (optional, to resolve the linked price table). Returns a list of `ProductSearchResult` (product id, description, unit price, available-stock indicator).

## Outbound ports required

- `ProductRepositoryPort` and `PriceTableRepositoryPort` (both owned by `masterdata` — not listed in M3's own outbound-ports table, only implied by the module spec's "Consumes from masterdata: product, price table" cross-module dependency; called here as a read-only query port, not a local M3 persistence port).

## REST endpoint

`GET /api/pdv/products/search?q=`

## Domain entities touched

- None owned by M3. Reads `Product` and `PriceTable` from `masterdata` — never mutated here.

## Acceptance criteria

- [ ] Matches by barcode (scanner or typed), internal code, or description.
- [ ] Responds in real time (300ms debounce); never requires pressing Enter to see results.
- [ ] Price shown reflects the customer's/channel's linked price table when a customer is set on the sale.
- [ ] Products with `status = OUT_OF_STOCK` or `INACTIVE` don't appear in results (doc §2.4).

## Dependencies

- **Depends on:** `masterdata` — `Product` and `PriceTable` must already be registered (M1).
- **Blocks:** [03 — Register Sale](uc-03-register-nfce-sale.md) — needs a way to find products to add to the cart.
