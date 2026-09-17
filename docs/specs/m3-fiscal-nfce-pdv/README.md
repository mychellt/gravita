# M3 — Fiscal: NFCe + PDV — Implementation Tickets

One file per use case, in build order. See the [module spec](../m3-fiscal-nfce-pdv.md) for the full functional/domain context these tickets are sliced from.

| # | Ticket | Phase | Responsibility |
|---|---|---|---|
| 01 | [Open POS Session](uc-01-open-pos-session.md) | 2 — Fiscal Core | Register open with operator and initial change amount. |
| 02 | [Search Product for Sale](uc-02-search-product-for-sale.md) | 2 — Fiscal Core | Barcode/code/description lookup, real-time. |
| 03 | [Register NFCe Sale](uc-03-register-nfce-sale.md) | 2 — Fiscal Core | Build the cart, apply discounts, take payments, compute change. |
| 04 | [Issue NFCe](uc-04-issue-nfce.md) | 2 — Fiscal Core | Tax calculation + SEFAZ authorization, online or contingency. |
| 05 | [Record Cash Movement](uc-05-record-cash-movement.md) | 2 — Fiscal Core | Sangria/Suprimento with mandatory justification. |
| 06 | [Close POS Session](uc-06-close-pos-session.md) | 2 — Fiscal Core | Reconciliation by payment method and Z-report generation. |
| 07 | [Cancel NFCe](uc-07-cancel-nfce.md) | 2 — Fiscal Core | Supervisor-authorized cancellation within the legal window. |
| 08 | [Sync Contingency Sales](uc-08-sync-contingency-sales.md) | 8 — Robustez | Background sync of pending sales once connectivity returns. |
| 09 | [Void Untransmitted Numbering](uc-09-void-untransmitted-numbering.md) | 8 — Robustez | End-of-day auto-void for numbering never sent. |

Tickets 01–07 form the online PDV flow (Phase 2); 08–09 are contingency/offline hardening (Phase 8), built once the online flow is proven.
