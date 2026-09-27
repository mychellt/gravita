# M7 — Vendas & CRM

Source: ERP MVP doc §8. Context package: `br.gravita.sales`.

## Purpose

The sales cycle goes from quote to invoicing without reopening the customer registration or re-typing data. The CRM tracks the relationship without bureaucracy.

## Functional requirements

### 8.1 Ciclo de Venda

- Orçamento (quote): created quickly; configurable validity; printed as PDF or sent via WhatsApp.
- Conversão: Quote → Order with one click; data fully preserved.
- Pedido de venda status: Draft → Approved → In separation → Invoiced → Cancelled.
- Aprovação: alçada by order value or discount percentage; remote approval via app.
- Reserva de estoque: automatic on order approval; released on cancellation.
- Faturamento: generates NFe, NFCe or NFSe depending on item configuration (product × service).
- Comissão: calculated per salesperson and per product; monthly commission report.
- Devolução de venda: a return NF-e is issued in the same flow; stock and financial positions are reverted.

### 8.2 CRM

- Funil de vendas: a visual Kanban by stage (Prospecting, Proposal, Negotiation, Closed, Lost).
- Oportunidades: estimated value, probability, expected close date, owner.
- Interações: log of calls, visits, e-mails, WhatsApp — with date, time, summarized content.
- Tarefas: follow-up scheduling with an app/e-mail alert; calendar integration.
- Segmentação: free tags, group, region, salesperson — for campaigns and reports.
- Follow-up automático: rules like "if no contact in X days, notify the salesperson" — no coding required.
- Metas de vendedor: monthly value and order-count target, with a tracking panel.
- Conversão do funil: conversion rate per stage, average cycle time, volume per salesperson.

## Domain model

- **Quote (Orçamento)** (aggregate root) — `customer`, `salesperson`, `items: [{product|service, quantity, unitPrice, discount}]`, `validUntil`, `status: {DRAFT, SENT, EXPIRED, CONVERTED}`. `salesperson` was added by UC-08 (see its Notes) — nothing before it needed to know who sold an order.
- **SalesOrder (Pedido de venda)** (aggregate root) — `originQuote` (optional), `customer`, `salesperson`, `items`, `discountPercent`, `status: {DRAFT, APPROVED, IN_SEPARATION, INVOICED, CANCELLED}`, `approval{alcada, approvedBy}`, `invoicedAt`. Invariant: stock is reserved exactly on the `DRAFT → APPROVED` transition and released on `→ CANCELLED`; invoicing is only possible from `APPROVED`/`IN_SEPARATION`, and records `invoicedAt`. `salesperson`/`invoicedAt` were added by UC-08, which needs both to attribute and period-filter commissions.
- **SalesInvoice** — the link from a `SalesOrder` to the fiscal document actually issued (`NfeDocument`, `NfceSale` or `NfseDocument` in `tax`), chosen per item type (product → NFe/NFCe, service → NFSe).
- **Commission** — `salesperson`, `product`, `order`, `rate`, `amount: Money`, aggregated monthly.
- **CommissionRate** — `salesperson`, `product`, `rate`, configured per pair; read by UC-08. Added by UC-08 (see its Notes) — no entity backed "the configured rate" the module spec's Notes on `Commission` already assumed.
- **SalesReturn (Devolução)** — `originalOrder`, `items`, `returnNfeRef`, reverts both `inventory` and `finance` positions.
- **Opportunity (Oportunidade)** (aggregate root, CRM) — `customer`, `stage: {PROSPECTING, PROPOSAL, NEGOTIATION, CLOSED, LOST}`, `estimatedValue: Money`, `probability`, `expectedCloseDate`, `owner`.
- **StageTransition** (immutable, append-only) — `opportunity`, `fromStage`, `toStage`, `timestamp`. Appended by `ManageOpportunityUseCase` on every stage change; the only source of "average cycle time" for `GetFunnelConversionUseCase`, since `Opportunity.stage` itself only holds the current value.
- **Interaction** — `opportunity`/`customer`, `channel: {CALL, VISIT, EMAIL, WHATSAPP}`, `summary`, `timestamp`.
- **FollowUpTask** — `dueDate`, `owner`, `alertChannel`, linked to an `Opportunity` or `Customer`.
- **FollowUpRule** — declarative rule ("no contact in X days → notify owner"), evaluated by a scheduled job, not hand-coded per case.
- **SalespersonTarget (Meta)** — `salesperson`, `month`, `valueTarget`, `orderCountTarget`.

## Use cases (`application.port.in`)

Each use case below has a standalone implementation ticket under [`m7-vendas-crm/`](m7-vendas-crm/README.md), ordered as the build sequence (order lifecycle first, then CRM).

| Use case | Responsibility |
|---|---|
| [`CreateQuoteUseCase`](m7-vendas-crm/uc-01-create-quote.md) | Fast quote creation, configurable validity. |
| [`SendQuoteUseCase`](m7-vendas-crm/uc-02-send-quote.md) | PDF or WhatsApp delivery. |
| [`ConvertQuoteToOrderUseCase`](m7-vendas-crm/uc-03-convert-quote-to-order.md) | One click, full data carry-over. |
| [`ApproveSalesOrderUseCase`](m7-vendas-crm/uc-04-approve-sales-order.md) | Alçada by value/discount; triggers stock reservation. |
| [`CancelSalesOrderUseCase`](m7-vendas-crm/uc-05-cancel-sales-order.md) | Triggers reservation release. |
| [`InvoiceSalesOrderUseCase`](m7-vendas-crm/uc-06-invoice-sales-order.md) | Picks NFe/NFCe/NFSe per item type, delegates issuance to `tax`. |
| [`ReturnSalesOrderUseCase`](m7-vendas-crm/uc-07-return-sales-order.md) | Return flow: NF-e issuance + stock/financial reversal. |
| [`CalculateCommissionUseCase`](m7-vendas-crm/uc-08-calculate-commission.md) | Per salesperson/product; feeds the monthly report. |
| [`ManageOpportunityUseCase`](m7-vendas-crm/uc-09-manage-opportunity.md) | CRUD + stage transitions on the Kanban funnel. |
| [`LogInteractionUseCase`](m7-vendas-crm/uc-10-log-interaction.md) | Record a CRM interaction. |
| [`ScheduleFollowUpTaskUseCase`](m7-vendas-crm/uc-11-schedule-follow-up-task.md) | Manual follow-up scheduling. |
| [`EvaluateFollowUpRulesUseCase`](m7-vendas-crm/uc-12-evaluate-follow-up-rules.md) | Scheduled job evaluating `FollowUpRule`s and notifying owners. |
| [`ManageFollowUpRuleUseCase`](m7-vendas-crm/uc-16-manage-follow-up-rule.md) | CRUD for `FollowUpRule` definitions — the rule data `EvaluateFollowUpRulesUseCase` reads. |
| [`SetSalespersonTargetUseCase`](m7-vendas-crm/uc-13-set-salesperson-target.md) | Monthly value/order-count target. |
| [`GetTargetProgressUseCase`](m7-vendas-crm/uc-14-get-target-progress.md) | Tracking panel against the target. |
| [`GetFunnelConversionUseCase`](m7-vendas-crm/uc-15-get-funnel-conversion.md) | Conversion rate/cycle-time/volume analytics. |

## Outbound ports (`application.port.out`)

| Port | Purpose |
|---|---|
| `QuoteRepositoryPort`, `SalesOrderRepositoryPort`, `OpportunityRepositoryPort`, `InteractionRepositoryPort`, `FollowUpTaskRepositoryPort`, `SalespersonTargetRepositoryPort` | Persistence. |
| `CommissionRepositoryPort` | Persistence for `Commission` — backs `CommissionJpaEntity`, already listed under Adapters. |
| `CommissionRateRepositoryPort` | Read-only access to the configured `CommissionRate` for a salesperson/product pair; backs `CommissionRateJpaEntity`. Added by UC-08 — see its Notes. |
| `StageTransitionRepositoryPort` | Append-only persistence for `StageTransition`; written by `ManageOpportunityUseCase`, read by `GetFunnelConversionUseCase`. |
| `FollowUpRuleRepositoryPort` | Persistence for `FollowUpRule` definitions. |
| `ReserveStockPort` / `ReleaseStockReservationPort` (into `inventory`) | Order approval/cancellation side effects. |
| `RegisterStockEntryPort` (into `inventory`) | Return flow: reverts stock as a customer-return entry (the same port M6 uses for purchase receipts, doc §6.1's "Entrada... devolução de cliente"). |
| `AdjustReceivableForReturnPort` (into `finance`) | Return flow: reduces or cancels the order's receivable. |
| `IssueFiscalDocumentPort` (into `tax`) | Dispatches to `IssueNfeUseCase`, `RegisterNfceSaleUseCase` or `ConvertRpsToNfseUseCase` based on item type. |
| `GenerateAccountsReceivablePort` (into `finance`) | Invoicing generates receivable titles. |
| `SendQuoteByWhatsAppPort` | Quote delivery (shared WhatsApp integration). |
| `SendFollowUpAlertPort` | App/e-mail follow-up notification. |

## Adapters

### Inbound (`adapter.in.web`)

| Method & path | Use case |
|---|---|
| `POST /api/sales/quotes` | `CreateQuoteUseCase` |
| `POST /api/sales/quotes/{id}/send` | `SendQuoteUseCase` |
| `POST /api/sales/quotes/{id}/convert` | `ConvertQuoteToOrderUseCase` |
| `POST /api/sales/orders/{id}/approve` | `ApproveSalesOrderUseCase` |
| `POST /api/sales/orders/{id}/cancel` | `CancelSalesOrderUseCase` |
| `POST /api/sales/orders/{id}/invoice` | `InvoiceSalesOrderUseCase` |
| `POST /api/sales/orders/{id}/return` | `ReturnSalesOrderUseCase` |
| `GET /api/sales/commissions?salesperson=&period=` | `CalculateCommissionUseCase` (report) |
| `GET/POST/PATCH /api/crm/opportunities` | `ManageOpportunityUseCase` |
| `POST /api/crm/opportunities/{id}/interactions` | `LogInteractionUseCase` |
| `POST /api/crm/follow-ups` | `ScheduleFollowUpTaskUseCase` |
| `GET/POST/PATCH /api/crm/follow-up-rules` | `ManageFollowUpRuleUseCase` |
| `GET/PUT /api/crm/targets/{salesperson}/{month}` | Target set/progress |
| `GET /api/crm/funnel/conversion` | `GetFunnelConversionUseCase` |

### Outbound (`adapter.out.persistence`)

`QuoteJpaEntity`, `SalesOrderJpaEntity`, `CommissionJpaEntity`, `CommissionRateJpaEntity`, `OpportunityJpaEntity`, `StageTransitionJpaEntity`, `InteractionJpaEntity`, `FollowUpTaskJpaEntity`, `FollowUpRuleJpaEntity`, `SalespersonTargetJpaEntity`.

## Cross-module dependencies

- **Consumes from `masterdata`**: customer, product, price table.
- **Consumes/provides with `inventory`**: reserve/release on approve/cancel.
- **Provides to `tax`**: order data for invoicing (M2/M3/M4, per item type).
- **Provides to `finance`**: accounts receivable on invoicing.
- **Provides to `purchasing`**: sales-order demand as a purchase-request trigger.
- **Provides to `reporting`**: funnel conversion, commissions, top products, target progress.

## Notes

- "Comissão calculada por vendedor e por produto" doesn't specify the rate source; modeled here as configured per salesperson/product pair, owned by `sales` rather than `masterdata`, since it's sales-policy rather than product-catalog data. Implementing UC-08 found that no entity/port actually backed this — see its Notes and the `CommissionRate` addition above.
- Gaps surfaced by the initial use-case split were patched in: a `RevertStockPort`/financial-reversal path for returns (resolved as `RegisterStockEntryPort` — reuses M5/M6's existing customer-return entry path — plus a new `AdjustReceivableForReturnPort` into a new M8 use case, `AdjustReceivableForReturnUseCase`); a missing `CommissionRepositoryPort`; a missing CRUD use case for `FollowUpRule` (added as `ManageFollowUpRuleUseCase`, uc-16); and a missing `StageTransition` log needed for "average cycle time" (added to the domain model, backed by `StageTransitionRepositoryPort`). Implementing UC-08 later surfaced two more, patched directly into UC-01/UC-03 and the domain model: no `salesperson` on `Quote`/`SalesOrder`, and no `CommissionRate` entity/port for "the configured rate" — see uc-08's Notes.
