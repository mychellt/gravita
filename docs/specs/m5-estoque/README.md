# M5 — Estoque: Implementation Tickets

One ticket per use case from the [M5 module spec](../m5-estoque.md), for granular implementation planning. All tickets belong to **Phase 3 — Operação** of the [build roadmap](../11-ux-e-decisoes-tecnicas.md#suggested-build-roadmap-doc-14).

| # | Ticket | Phase | Responsibility |
|---|---|---|---|
| 1 | [Get Stock Balance](uc-01-get-stock-balance.md) | 3 — Operação | Read current balance for a product, any warehouse or a specific one. |
| 2 | [Register Stock Entry](uc-02-register-stock-entry.md) | 3 — Operação | Increase balance and CMV on purchase receipt / customer return. |
| 3 | [Register Stock Exit](uc-03-register-stock-exit.md) | 3 — Operação | Decrease balance on confirmed sale / supplier return. |
| 4 | [Adjust Inventory](uc-04-adjust-inventory.md) | 3 — Operação | Manual positive/negative correction with mandatory justification. |
| 5 | [Transfer Stock](uc-05-transfer-stock.md) | 3 — Operação | Move stock between warehouses/branches, with in-transit tracking. |
| 6 | [Reserve Stock](uc-06-reserve-stock.md) | 3 — Operação | Reserve quantity for an approved sales order. |
| 7 | [Release Stock Reservation](uc-07-release-stock-reservation.md) | 3 — Operação | Release a reservation on order cancellation. |
| 8 | [Start Physical Count](uc-08-start-physical-count.md) | 3 — Operação | Open a partial (by group) or total physical count. |
| 9 | [Approve Physical Count](uc-09-approve-physical-count.md) | 3 — Operação | Approve a count, auto-generating divergence adjustments. |
| 10 | [Check Expiring Lots](uc-10-check-expiring-lots.md) | 3 — Operação | Surface lots expiring within a configurable window. |
| 11 | [Suggest Reorder](uc-11-suggest-reorder.md) | 3 — Operação | Emit a reorder suggestion when balance hits the reorder point. |

Suggested build order within the phase: 1 → 2 → 3 → 4 → (5, 6 → 7 in any order) → 8 → 9 → 10 → 11, since later tickets' acceptance criteria assume the read/write primitives from 1–4 exist.
