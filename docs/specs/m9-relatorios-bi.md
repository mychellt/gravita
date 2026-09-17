# M9 — Relatórios & BI

Source: ERP MVP doc §10. Context package: `br.gravita.reporting`.

## Purpose

All reports are filterable, exportable to PDF and Excel, and accessible per permission profile. The executive dashboard must load in under 3 seconds even with a large data volume.

## Functional requirements

### 10.1 Dashboard Executivo

- Faturamento: total for day/week/month and comparison to the prior period, as a line chart.
- CMV e Margem: cost of goods sold and gross margin percentage, per period.
- Inadimplência: total open-overdue and a simplified aging bucket (≤30 / 31–60 / +60 days).
- Estoque crítico: products below minimum and near expiry — a clickable list.
- Top produtos: top-10 ranking by quantity and by value in the period.
- Metas de vendas: progress bar per salesperson and for the company, current month.

### 10.2 Relatórios por Módulo

- Curva ABC: products and customers classified by revenue representativeness (A/B/C).
- DRE Gerencial: gross revenue, deductions, CMV, expenses by cost center, net result.
- Giro de estoque: turnover per product in the period; identification of stalled items.
- Comissões: per salesperson, per product, per period — ready for payroll.
- Compras por fornecedor: volume, value and average delivery time per supplier in the period.
- Livros fiscais: Entries, Exits and ICMS Assessment — generated per period, in PDF and TXT.
- Tributos apurados: ICMS, IPI, PIS, COFINS, ISS per period — for accountant review.

## Domain model

This module is read-model heavy: it owns no transactional aggregates of its own and instead projects data already owned by other contexts. It's modeled as query services over read models, not as domain aggregates with invariants.

- **ExecutiveDashboardView** — a composed read model of revenue (from `sales`/`tax`), CMV/margin (from `inventory`), delinquency (from `finance`), critical stock (from `inventory`), top products (from `sales`), target progress (from `sales`).
- **AbcCurveEntry** — `product|customer`, `revenueShare`, `class: {A, B, C}`.
- **ManagerialDre (DRE Gerencial)** — `grossRevenue`, `deductions`, `cmv`, `expensesByCostCenter`, `netResult`, per period.
- **StockTurnoverEntry** — `product`, `turnoverRate`, `stalledFlag`, per period.
- **CommissionReportEntry** — projection of `sales.Commission`.
- **SupplierPurchaseSummary** — `supplier`, `volume`, `value`, `averageLeadTimeDays`.
- **FiscalBookEntry** / **AssessedTaxSummary** — projections of `tax` module data (Entries/Exits/ICMS Assessment books, ICMS/IPI/PIS/COFINS/ISS totals per period).

## Use cases (`application.port.in`)

Each use case below has a standalone implementation ticket under [`m9-relatorios-bi/`](m9-relatorios-bi/README.md).

| Use case | Responsibility |
|---|---|
| [`GetExecutiveDashboardUseCase`](m9-relatorios-bi/uc-01-get-executive-dashboard.md) | Composed dashboard, must resolve in < 3s (performance budget, doc §10). |
| [`GetAbcCurveUseCase`](m9-relatorios-bi/uc-02-get-abc-curve.md) | Product/customer classification by period. |
| [`GetManagerialDreUseCase`](m9-relatorios-bi/uc-03-get-managerial-dre.md) | DRE gerencial by period/cost center. |
| [`GetStockTurnoverUseCase`](m9-relatorios-bi/uc-04-get-stock-turnover.md) | Turnover and stalled-item detection. |
| [`GetCommissionReportUseCase`](m9-relatorios-bi/uc-05-get-commission-report.md) | Per salesperson/product/period. |
| [`GetSupplierPurchaseSummaryUseCase`](m9-relatorios-bi/uc-06-get-supplier-purchase-summary.md) | Volume/value/lead time per supplier. |
| [`GetFiscalBooksUseCase`](m9-relatorios-bi/uc-07-get-fiscal-books.md) | Entries/Exits/ICMS Assessment, PDF + TXT. |
| [`GetAssessedTaxesUseCase`](m9-relatorios-bi/uc-08-get-assessed-taxes.md) | ICMS/IPI/PIS/COFINS/ISS per period. |
| [`ExportReportUseCase`](m9-relatorios-bi/uc-09-export-report.md) | Generic PDF/Excel export, applied to any of the above. |

## Outbound ports (`application.port.out`)

| Port | Purpose |
|---|---|
| `SalesReadModelPort`, `InventoryReadModelPort`, `FinanceReadModelPort`, `TaxReadModelPort`, `PurchasingReadModelPort` | Read-only query ports into the other contexts — each context exposes a reporting read port rather than `reporting` reaching into their repositories directly. |
| `RenderPdfPort`, `RenderExcelPort` | Export rendering. |
| `PermissionCheckPort` (from `system`) | Report/screen visibility per profile. |

## Adapters

### Inbound (`adapter.in.web`)

| Method & path | Use case |
|---|---|
| `GET /api/reports/dashboard` | `GetExecutiveDashboardUseCase` |
| `GET /api/reports/abc-curve?type=product\|customer&period=` | `GetAbcCurveUseCase` |
| `GET /api/reports/dre?period=&costCenter=` | `GetManagerialDreUseCase` |
| `GET /api/reports/stock-turnover?period=` | `GetStockTurnoverUseCase` |
| `GET /api/reports/commissions?salesperson=&period=` | `GetCommissionReportUseCase` |
| `GET /api/reports/purchases-by-supplier?period=` | `GetSupplierPurchaseSummaryUseCase` |
| `GET /api/reports/fiscal-books?period=` | `GetFiscalBooksUseCase` |
| `GET /api/reports/assessed-taxes?period=` | `GetAssessedTaxesUseCase` |
| `GET /api/reports/{reportId}/export?format=pdf\|xlsx` | `ExportReportUseCase` |

### Outbound (`adapter.out.persistence`)

No entities of its own; adapters implement the read-model ports as query classes against each context's schema (or a dedicated reporting read replica/materialized view, if load requires it — not specified by the source doc).

## Cross-module dependencies

- **Consumes from every other context** via read-only ports (`sales`, `inventory`, `finance`, `tax`, `purchasing`).
- **Consumes from `system`**: permission checks for report/screen access.

## Notes

- The "< 3 seconds even with large data volume" requirement (doc §10 intro) is a performance budget, not a design; whether it's met with direct queries, materialized views, or a reporting read replica is an implementation decision left open by the source doc and should be settled once real data volumes are known.
- This module has no aggregates because the PDF describes it purely as reporting over data owned elsewhere — modeling it with its own mutable state would duplicate source-of-truth data that already lives in `sales`, `inventory`, `finance` and `tax`.
