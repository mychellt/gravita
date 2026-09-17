# UC-M9-06 — Get Supplier Purchase Summary (`GetSupplierPurchaseSummaryUseCase`)

**Module:** M9 — Relatórios & BI ([module spec](../m9-relatorios-bi.md))
**Context package:** `br.gravita.reporting`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

Doc §10.2 — Compras por fornecedor: volume, value and average delivery time per supplier in the period.

## Description

Triggered when a user requests purchasing performance by supplier for a period. The use case pulls purchase order and receipt data from `purchasing` and aggregates volume, value and average delivery time per supplier. Read-only.

## Port signature

```java
public interface GetSupplierPurchaseSummaryUseCase {
    List<SupplierPurchaseSummary> execute(SupplierPurchaseSummaryQuery query);
}
```

`SupplierPurchaseSummaryQuery` fields: `period`. Returns a list of `SupplierPurchaseSummary`, each with `supplier`, `volume`, `value`, `averageLeadTimeDays`.

## Outbound ports required

- `PurchasingReadModelPort` — purchase orders and receipts for the period

## REST endpoint

`GET /api/reports/purchases-by-supplier?period=`

## Domain entities touched

- `SupplierPurchaseSummary`

## Acceptance criteria

- [ ] Returns volume and value purchased per supplier for the requested period.
- [ ] Returns average delivery lead time per supplier for the requested period.

## Dependencies

- **Depends on:** `purchasing` read-model port.
- **Blocks:** —
