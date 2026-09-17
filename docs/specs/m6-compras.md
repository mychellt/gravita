# M6 — Compras (Purchasing)

Source: ERP MVP doc §7. Context package: `br.gravita.purchasing`.

## Purpose

Purchasing closes the replenishment cycle: from a need identified in stock to the physical and financial entry of merchandise.

## Functional requirements

- Solicitação de compra: created by a user, by the min-stock trigger, or by demand from a sales order.
- Cotação: the item list is sent to multiple suppliers, with side-by-side price/deadline comparison.
- Aprovação: configurable approval alçada by value, with an approval workflow via app/e-mail.
- Pedido de compra: status Open / Partially Received / Closed / Cancelled.
- Recebimento: total or partial, with physical conference before confirming the entry in the system.
- Importação de XML: the supplier's NF-e is auto-imported at receiving — zero re-typing.
- Contas a pagar: auto-generated on confirming receipt, using the NF's installment terms.
- Devolução: partial or total return to the supplier; a return NF-e is issued in the same flow.

## Domain model

- **PurchaseRequest (Solicitação)** (aggregate root) — `origin: {USER, MIN_STOCK_TRIGGER, SALES_ORDER_DEMAND}`, `items: [{product, quantity}]`, `requestedBy`, `status: {OPEN, QUOTED, CONVERTED, CANCELLED}`.
- **Quotation (Cotação)** — `request: PurchaseRequestRef`, `suppliers: [SupplierRef]`, `responses: [QuotationResponse {supplier, itemPrices, deadline}]` — compared side by side to pick a winner.
- **PurchaseOrder (Pedido de compra)** (aggregate root) — `supplier`, `items: [{product, quantity, unitPrice}]`, `approvalAlcada` (resolved from value against `masterdata`/`system` config), `status: {OPEN, PARTIALLY_RECEIVED, CLOSED, CANCELLED}`, `receipts: [PurchaseReceipt]`.
- **PurchaseReceipt (Recebimento)** — `order: PurchaseOrderRef`, `receivedItems: [{product, orderedQty, receivedQty}]`, `conferenceResult` (divergences between ordered/physically-arrived/on-NF, cross-referencing M2's `InboundNfe`), `total`/`partial` flag.
- **PurchaseReturn (Devolução)** — `originalReceipt`, `items`, `partial`/`total` flag, `returnNfeRef` (issued via M2 in the same flow).

## Use cases (`application.port.in`)

Each use case below has a standalone implementation ticket under [`m6-compras/`](m6-compras/README.md), ordered as the build sequence.

| Use case | Responsibility |
|---|---|
| [`CreatePurchaseRequestUseCase`](m6-compras/uc-01-create-purchase-request.md) | Manual, min-stock-triggered (from `inventory`'s `SuggestReorderUseCase`), or sales-demand-triggered. |
| [`SendQuotationUseCase`](m6-compras/uc-02-send-quotation.md) | Sends a request's item list to selected suppliers for pricing. |
| [`RegisterQuotationResponseUseCase`](m6-compras/uc-03-register-quotation-response.md) | Records one supplier's price/deadline reply, for side-by-side comparison. |
| [`CreatePurchaseOrderUseCase`](m6-compras/uc-04-create-purchase-order.md) | From an approved/quoted request. |
| [`ApprovePurchaseOrderUseCase`](m6-compras/uc-05-approve-purchase-order.md) | Alçada-based approval workflow. |
| [`ReceivePurchaseOrderUseCase`](m6-compras/uc-06-receive-purchase-order.md) | Total or partial receipt, physical conference. |
| [`ImportSupplierNfeAtReceivingUseCase`](m6-compras/uc-07-import-supplier-nfe-at-receiving.md) | Delegates XML parsing to M2's `ImportSupplierNfeXmlUseCase`, then reconciles against the order. |
| [`ConfirmPurchaseReceiptUseCase`](m6-compras/uc-08-confirm-purchase-receipt.md) | Triggers `inventory`'s stock entry and `finance`'s payable generation. |
| [`ReturnToSupplierUseCase`](m6-compras/uc-09-return-to-supplier.md) | Partial/total return, triggers a return NF-e via M2. |

## Outbound ports (`application.port.out`)

| Port | Purpose |
|---|---|
| `PurchaseRequestRepositoryPort`, `QuotationRepositoryPort`, `PurchaseOrderRepositoryPort`, `PurchaseReceiptRepositoryPort` | Persistence. |
| `NotifyApprovalWorkflowPort` | App/e-mail approval notifications. |
| `RegisterStockEntryPort` (into `inventory`) | Confirmed receipt → stock entry. |
| `GeneratePayableFromReceiptPort` (into `finance`) | Confirmed receipt → accounts payable, installments from the NF terms. |
| `IssueNfeUseCase` (from M2, for returns) | Return NF-e issuance in the same flow. |

## Adapters

### Inbound (`adapter.in.web`)

| Method & path | Use case |
|---|---|
| `POST /api/purchasing/requests` | `CreatePurchaseRequestUseCase` |
| `POST /api/purchasing/requests/{id}/quotations` | `SendQuotationUseCase` |
| `POST /api/purchasing/quotations/{id}/responses` | `RegisterQuotationResponseUseCase` |
| `POST /api/purchasing/orders` | `CreatePurchaseOrderUseCase` |
| `POST /api/purchasing/orders/{id}/approve` | `ApprovePurchaseOrderUseCase` |
| `POST /api/purchasing/orders/{id}/receipts` | `ReceivePurchaseOrderUseCase` + XML import |
| `POST /api/purchasing/receipts/{id}/confirm` | `ConfirmPurchaseReceiptUseCase` |
| `POST /api/purchasing/receipts/{id}/return` | `ReturnToSupplierUseCase` |

### Outbound (`adapter.out.persistence`)

`PurchaseRequestJpaEntity`, `QuotationJpaEntity`, `PurchaseOrderJpaEntity`, `PurchaseReceiptJpaEntity`, `PurchaseReturnJpaEntity`.

## Cross-module dependencies

- **Consumes from `inventory`**: reorder suggestions (`SuggestReorderUseCase`).
- **Consumes from `masterdata`**: supplier, product, default purchase CFOP.
- **Consumes from `tax` (M2)**: supplier XML import, return NF-e issuance.
- **Provides to `inventory`**: stock entry on confirmed receipt.
- **Provides to `finance`**: accounts payable on confirmed receipt.

## Notes

- "Approval alçada" is referenced both here and in M7/M8; the doc doesn't say whether it's one shared configuration or per-module. It's modeled as a single `system`-owned alçada configuration (see [m10-sistema.md](m10-sistema.md)) that each module resolves against, to avoid three divergent implementations.
