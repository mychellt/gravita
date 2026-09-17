# M6 — Compras: Implementation Tickets

Source: [`m6-compras.md`](../m6-compras.md) (ERP MVP doc §7). Each row is a standalone, independently implementable use case ticket. Order below is the build sequence — earlier tickets are dependencies of later ones (see each ticket's "Dependencies" section).

| # | Ticket | Phase | Responsibility |
|---|---|---|---|
| 01 | [Create Purchase Request](uc-01-create-purchase-request.md) | 3 — Operação | Open a request manually, from min-stock trigger, or from sales demand. |
| 02 | [Send Quotation](uc-02-send-quotation.md) | 3 — Operação | Send a request's item list to selected suppliers for pricing. |
| 03 | [Register Quotation Response](uc-03-register-quotation-response.md) | 3 — Operação | Record one supplier's price/deadline reply. |
| 04 | [Create Purchase Order](uc-04-create-purchase-order.md) | 3 — Operação | Convert a (quoted) request into an order against one supplier. |
| 05 | [Approve Purchase Order](uc-05-approve-purchase-order.md) | 3 — Operação | Alçada-based approval workflow via app/e-mail. |
| 06 | [Receive Purchase Order](uc-06-receive-purchase-order.md) | 3 — Operação | Record physical conference, total or partial. |
| 07 | [Import Supplier NFe at Receiving](uc-07-import-supplier-nfe-at-receiving.md) | 3 — Operação | Auto-fill and reconcile from the supplier's XML. |
| 08 | [Confirm Purchase Receipt](uc-08-confirm-purchase-receipt.md) | 3 — Operação | Finalize receipt: triggers stock entry and payable generation. |
| 09 | [Return to Supplier](uc-09-return-to-supplier.md) | 3 — Operação | Partial/total return with return NF-e issued in the same flow. |

Note: tickets 02 and 03 correspond to `m6-compras.md`'s single combined row (`SendQuotationUseCase` / `RegisterQuotationResponseUseCase`), split here since they're independently implementable steps of the same flow.
