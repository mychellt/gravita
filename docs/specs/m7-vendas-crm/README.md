# M7 — Vendas & CRM: Implementation Tickets

Index of standalone, per-use-case implementation tickets for [M7 — Vendas & CRM](../m7-vendas-crm.md). All tickets belong to **Phase 4 — Comercial** of the roadmap (doc §14); order below is the suggested build sequence — the order lifecycle (01–08) before CRM (09–15).

| # | Ticket | Phase | Responsibility |
|---|---|---|---|
| 01 | [Create Quote](uc-01-create-quote.md) | Phase 4 | Fast quote creation, configurable validity. |
| 02 | [Send Quote](uc-02-send-quote.md) | Phase 4 | PDF or WhatsApp delivery. |
| 03 | [Convert Quote to Order](uc-03-convert-quote-to-order.md) | Phase 4 | One click, full data carry-over. |
| 04 | [Approve Sales Order](uc-04-approve-sales-order.md) | Phase 4 | Alçada by value/discount; triggers stock reservation. |
| 05 | [Cancel Sales Order](uc-05-cancel-sales-order.md) | Phase 4 | Triggers reservation release. |
| 06 | [Invoice Sales Order](uc-06-invoice-sales-order.md) | Phase 4 | Picks NFe/NFCe/NFSe per item type, delegates issuance to `tax`. |
| 07 | [Return Sales Order](uc-07-return-sales-order.md) | Phase 4 | Return flow: NF-e issuance + stock/financial reversal. |
| 08 | [Calculate Commission](uc-08-calculate-commission.md) | Phase 4 | Per salesperson/product; feeds the monthly report. |
| 09 | [Manage Opportunity](uc-09-manage-opportunity.md) | Phase 4 | CRUD + stage transitions on the Kanban funnel. |
| 10 | [Log Interaction](uc-10-log-interaction.md) | Phase 4 | Record a CRM interaction. |
| 11 | [Schedule Follow-up Task](uc-11-schedule-follow-up-task.md) | Phase 4 | Manual follow-up scheduling. |
| 12 | [Evaluate Follow-up Rules](uc-12-evaluate-follow-up-rules.md) | Phase 4 | Scheduled job evaluating `FollowUpRule`s and notifying owners. |
| 16 | [Manage Follow-up Rule](uc-16-manage-follow-up-rule.md) | Phase 4 | CRUD for `FollowUpRule` definitions — build before 12. |
| 13 | [Set Salesperson Target](uc-13-set-salesperson-target.md) | Phase 4 | Monthly value/order-count target. |
| 14 | [Get Target Progress](uc-14-get-target-progress.md) | Phase 4 | Tracking panel against the target. |
| 15 | [Get Funnel Conversion](uc-15-get-funnel-conversion.md) | Phase 4 | Conversion rate/cycle-time/volume analytics. |

## Patched gaps

Four gaps surfaced when this module was first split into tickets, now resolved:

- **07** (return) had no port to revert stock/financial state — resolved by reusing M5's existing customer-return stock-entry path (`RegisterStockEntryPort`) and a new M8 use case, `AdjustReceivableForReturnUseCase` ([m8-financeiro/uc-21](../m8-financeiro/uc-21-adjust-receivable-for-return.md)).
- **08** (commission) was missing `CommissionRepositoryPort` — added to the module's outbound-ports table.
- **12** (follow-up rules) had no CRUD for `FollowUpRule` — added as **16**, which must ship before 12 can evaluate anything.
- **15** (funnel conversion) needed stage-transition history for "average cycle time," which didn't exist — added a `StageTransition` entity to the domain model, appended by **09** on every stage change.
