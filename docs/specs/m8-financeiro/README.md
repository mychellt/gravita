# M8 — Financeiro: Implementation Tickets

One ticket per use case from [m8-financeiro.md](../m8-financeiro.md), for granular implementation planning. All belong to **Phase 5 — Financeiro** (doc §14).

## Receivables (doc §9.1)

| # | Ticket | Responsibility |
|---|---|---|
| 01 | [Generate Receivable from Invoicing](uc-01-generate-receivable-from-invoicing.md) | Auto-create titles on M7 invoicing / M4 NFSe authorization. |
| 02 | [Create Manual Receivable](uc-02-create-manual-receivable.md) | One-off charges. |
| 03 | [Generate Boleto](uc-03-generate-boleto.md) | Boleto creation + e-mail delivery. |
| 04 | [Generate PIX Charge](uc-04-generate-pix-charge.md) | Dynamic QR code per title. |
| 05 | [Import Bank Return](uc-05-import-bank-return.md) | Daily CNAB 240/400 automatic settlement. |
| 06 | [Settle Title Manually](uc-06-settle-title-manually.md) | Total/partial baixa with interest/fine/discount/surcharge. |
| 07 | [Renegotiate Title](uc-07-renegotiate-title.md) | Overdue title → new installment plan. |
| 08 | [Get Aging List](uc-08-get-aging-list.md) | Delinquency report by overdue range. |
| 09 | [Get Customer Statement](uc-09-get-customer-statement.md) | Per-customer title/settlement/renegotiation history. |
| 21 | [Adjust Receivable for Return](uc-21-adjust-receivable-for-return.md) | Reduces/cancels a receivable when M7 registers a sales return. |

## Payables (doc §9.2)

| # | Ticket | Responsibility |
|---|---|---|
| 10 | [Create Manual Payable](uc-10-create-manual-payable.md) | One-off expenses. |
| 11 | [Generate Payable from Receipt](uc-11-generate-payable-from-receipt.md) | Auto-create payable on M6 confirmed receipt. |
| 12 | [Approve Payable](uc-12-approve-payable.md) | Alçada-based approval. |
| 13 | [Batch Pay](uc-13-batch-pay.md) | Multi-title selection → CNAB remittance. |
| 22 | [Confirm Batch Payment](uc-22-confirm-batch-payment.md) | Imports the bank's CNAB return for a remittance; settles confirmed payables. |
| 14 | [Pay via PIX](uc-14-pay-via-pix.md) | Direct transfer + receipt storage. |
| 15 | [Attach Payable Document](uc-15-attach-payable-document.md) | Upload boleto/NF/receipt. |
| 16 | [Split Payable by Cost Center](uc-16-split-payable-by-cost-center.md) | Percentage-based expense split. |

## Cash flow & reconciliation (doc §9.3)

| # | Ticket | Responsibility |
|---|---|---|
| 17 | [Get Cash Flow](uc-17-get-cash-flow.md) | Daily/weekly/monthly view with projection, filterable. |
| 18 | [Reconcile Bank Statement](uc-18-reconcile-bank-statement.md) | OFX/CSV import + auto-match. |
| 19 | [Record Internal Cash Movement](uc-19-record-internal-cash-movement.md) | Transfers between internal cash box and bank. |
| 20 | [Close Daily Cash](uc-20-close-daily-cash.md) | Daily summary per account. |

## Build-order notes

- Within Receivables, 01/02 must land before 03/04/06/07/08/09; 05 depends on 03 (a boleto to return on); 21 depends on M7's return flow existing (Phase 4).
- Within Payables, 10/11 must land before 12; 12 gates 13/14; 22 depends on 13 (a remittance to return on).
- Cash flow (17) needs both a receivable- and payable-creation ticket done; reconciliation (18) needs settlement/cash-movement tickets (05, 06, 14, 19) in place first; 20 depends on 19.
- 21 and 22 were added after the initial split — they close the two gaps originally flagged here: no payable-side settlement symmetrical to 05 (now 22), and no reversal of a receivable on a sales return (now 21). `InternalCashBoxRepositoryPort` is now in the module spec's outbound-ports table (used by 19/20).
