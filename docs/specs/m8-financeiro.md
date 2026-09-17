# M8 — Financeiro

Source: ERP MVP doc §9. Context package: `br.gravita.finance`.

## Purpose

Covers the full payment/receipt cycle with real-time cash-flow visibility. Bank integration removes re-typing and reconciliation errors.

## Functional requirements

### 9.1 Contas a Receber

- Origem: invoicing (automatic) or manual entry for one-off charges.
- Boleto bancário: generation and e-mail delivery; integration with major banks via API (Itaú, BB, Bradesco, Sicoob, Sicredi).
- PIX Cobrança: dynamic QR code per title, with due date and amount; automatic confirmation.
- Baixa automática: daily-imported bank return (CNAB 240/400), reconciled automatically.
- Baixa manual: total or partial, with fields for interest, fine, discount and surcharge.
- Renegociação: an overdue title converted into a new installment plan, via a simple flow.
- Aging list: delinquency report by overdue range (0–30, 31–60, 61–90, +90 days).
- Extrato por cliente: history of titles, settlements, renegotiations and open balance per customer.

### 9.2 Contas a Pagar

- Origem: purchase receipt (automatic) or manual entry for expenses.
- Aprovação: alçada by value, remote approval via e-mail or app.
- Pagamento em lote: multiple-title selection, CNAB remittance generation for the bank.
- PIX pagamento: direct transfer via bank integration; receipt auto-saved.
- Anexo: upload of a boleto, NF or receipt linked to the title.
- Centro de custo: expense split across cost centers with configurable percentages.
- Relatórios: due, overdue, paid — filterable by supplier, cost center and period.

### 9.3 Fluxo de Caixa e Conciliação

- Visão do fluxo: daily, weekly, monthly; realized entries/exits plus a projection of open items.
- Filtros: by company, branch, bank account and cost center.
- Alerta de saldo: automatic notification when the projected balance goes negative.
- Conciliação bancária: OFX/CSV statement import, with automatic matching by value and date.
- Caixa interno: the back office's physical cash box, separate from the PDV; transfers between cash and bank.
- Fechamento diário: daily summary — entries, exits, opening and closing balance per account.

## Domain model

- **Receivable (Título a receber)** (aggregate root) — `customer`, `origin: {INVOICING, MANUAL}`, `amount: Money`, `dueDate`, `installments`, `status: {OPEN, PARTIALLY_SETTLED, SETTLED, RENEGOTIATED, CANCELLED}`, `settlements: [Settlement]`.
- **Payable (Título a pagar)** (aggregate root) — `supplier`, `origin: {PURCHASE_RECEIPT, MANUAL}`, `amount: Money`, `dueDate`, `costCenterSplit: [{costCenter, percent}]`, `approval{alcada, approvedBy}`, `status: {OPEN, APPROVED, PAID, CANCELLED}`, `attachments: [URL]`.
- **Settlement (Baixa)** — `title` (receivable or payable), `amount`, `interest`, `fine`, `discount`, `surcharge`, `method: {AUTOMATIC_CNAB, MANUAL, PIX}`, `timestamp`.
- **Boleto** — `receivable`, `bankIntegration`, `barcodeLine`, `status`.
- **PixCharge** — `receivable`, `dynamicQrPayload`, `expiresAt`, `status: {PENDING, PAID, EXPIRED}`.
- **Renegotiation** — `originalReceivable`, `newInstallmentPlan: [Receivable]`.
- **BankStatementLine** — imported OFX/CSV entry; matched (or not) to a `Settlement`/`CashMovement`.
- **CashFlowProjection** — read model combining realized `Settlement`s and open `Receivable`/`Payable` titles by date bucket.
- **InternalCashBox (Caixa interno)** — separate from the PDV's `PosSession` (M3); `CashMovement` (transfer to/from bank), `DailyClosing`.

## Use cases (`application.port.in`)

Each use case below has a standalone implementation ticket under [`m8-financeiro/`](m8-financeiro/README.md), for granular phase planning.

### Receivables (§9.1)

| Use case | Responsibility |
|---|---|
| [`GenerateReceivableFromInvoicingUseCase`](m8-financeiro/uc-01-generate-receivable-from-invoicing.md) | Auto-create titles on M7 invoicing (and M4 NFSe). |
| [`CreateManualReceivableUseCase`](m8-financeiro/uc-02-create-manual-receivable.md) | One-off charges. |
| [`GenerateBoletoUseCase`](m8-financeiro/uc-03-generate-boleto.md) | Boleto creation + e-mail delivery. |
| [`GeneratePixChargeUseCase`](m8-financeiro/uc-04-generate-pix-charge.md) | Dynamic QR code per title. |
| [`ImportBankReturnUseCase`](m8-financeiro/uc-05-import-bank-return.md) (CNAB 240/400) | Daily automatic settlement/reconciliation. |
| [`SettleTitleManuallyUseCase`](m8-financeiro/uc-06-settle-title-manually.md) | Total/partial baixa with interest/fine/discount/surcharge. |
| [`RenegotiateTitleUseCase`](m8-financeiro/uc-07-renegotiate-title.md) | Overdue title → new installment plan. |
| [`GetAgingListUseCase`](m8-financeiro/uc-08-get-aging-list.md) | Delinquency report by overdue range. |
| [`GetCustomerStatementUseCase`](m8-financeiro/uc-09-get-customer-statement.md) | Per-customer title/settlement/renegotiation history. |
| [`AdjustReceivableForReturnUseCase`](m8-financeiro/uc-21-adjust-receivable-for-return.md) | Reduces or cancels a receivable when M7 registers a sales return. |

### Payables (§9.2)

| Use case | Responsibility |
|---|---|
| [`CreateManualPayableUseCase`](m8-financeiro/uc-10-create-manual-payable.md) | One-off expenses. |
| [`GeneratePayableFromReceiptUseCase`](m8-financeiro/uc-11-generate-payable-from-receipt.md) | Auto-create payable on M6 confirmed receipt. |
| [`ApprovePayableUseCase`](m8-financeiro/uc-12-approve-payable.md) | Alçada-based approval. |
| [`BatchPayUseCase`](m8-financeiro/uc-13-batch-pay.md) | Multi-title selection → CNAB remittance. |
| [`ConfirmBatchPaymentUseCase`](m8-financeiro/uc-22-confirm-batch-payment.md) | Imports the bank's CNAB return for a payment remittance; settles the confirmed payables. |
| [`PayViaPixUseCase`](m8-financeiro/uc-14-pay-via-pix.md) | Direct transfer + receipt storage. |
| [`AttachPayableDocumentUseCase`](m8-financeiro/uc-15-attach-payable-document.md) | Upload boleto/NF/receipt. |
| [`SplitPayableByCostCenterUseCase`](m8-financeiro/uc-16-split-payable-by-cost-center.md) | Percentage-based split. |

### Cash flow & reconciliation (§9.3)

| Use case | Responsibility |
|---|---|
| [`GetCashFlowUseCase`](m8-financeiro/uc-17-get-cash-flow.md) | Daily/weekly/monthly view with projection, filterable. |
| [`ReconcileBankStatementUseCase`](m8-financeiro/uc-18-reconcile-bank-statement.md) | OFX/CSV import + auto-match. |
| [`RecordInternalCashMovementUseCase`](m8-financeiro/uc-19-record-internal-cash-movement.md) | Transfers between internal cash box and bank. |
| [`CloseDailyCashUseCase`](m8-financeiro/uc-20-close-daily-cash.md) | Daily summary per account. |

## Outbound ports (`application.port.out`)

| Port | Purpose |
|---|---|
| `ReceivableRepositoryPort`, `PayableRepositoryPort`, `SettlementRepositoryPort`, `BankStatementRepositoryPort` | Persistence. |
| `BankIntegrationPort` | Boleto issuance, PIX cobrança/pagamento, CNAB remittance/return — implementations per bank (Itaú, BB, Bradesco, Sicoob, Sicredi). |
| `ImportBankStatementPort` | OFX/CSV parsing. |
| `NotifyNegativeBalanceProjectionPort` | Cash-flow alerting. |
| `DocumentAttachmentStoragePort` | Payable attachments. |
| `UpdateCustomerCreditStatusPort` (into `masterdata`) | Keeps `Customer.status` current from delinquency. |
| `InternalCashBoxRepositoryPort` | Persistence for `InternalCashBox`, `CashMovement` and `DailyClosing` — the back-office cash box (§9.3), separate from `ReceivableRepositoryPort`/`PayableRepositoryPort`. |

## Adapters

### Inbound (`adapter.in.web`)

| Method & path | Use case |
|---|---|
| `POST /api/finance/receivables` | Manual receivable |
| `POST /api/finance/receivables/{id}/boleto` | `GenerateBoletoUseCase` |
| `POST /api/finance/receivables/{id}/pix-charge` | `GeneratePixChargeUseCase` |
| `POST /api/finance/receivables/{id}/settle` | `SettleTitleManuallyUseCase` |
| `POST /api/finance/receivables/{id}/renegotiate` | `RenegotiateTitleUseCase` |
| `POST /api/finance/receivables/{id}/adjust-for-return` | `AdjustReceivableForReturnUseCase` |
| `GET /api/finance/receivables/aging` | `GetAgingListUseCase` |
| `GET /api/finance/customers/{id}/statement` | `GetCustomerStatementUseCase` |
| `POST /api/finance/payables` | Manual payable |
| `POST /api/finance/payables/{id}/approve` | `ApprovePayableUseCase` |
| `POST /api/finance/payables/batch-pay` | `BatchPayUseCase` |
| `POST /api/finance/payables/{id}/pix-pay` | `PayViaPixUseCase` |
| `POST /api/finance/payables/{id}/attachments` | `AttachPayableDocumentUseCase` |
| `GET /api/finance/cash-flow` | `GetCashFlowUseCase` |
| `POST /api/finance/bank-statements/import` | `ReconcileBankStatementUseCase` |
| `POST /api/finance/internal-cash/movements` | `RecordInternalCashMovementUseCase` |
| `POST /api/finance/internal-cash/close` | `CloseDailyCashUseCase` |

### Outbound (`adapter.out.persistence` / integrations)

`ReceivableJpaEntity`, `PayableJpaEntity`, `SettlementJpaEntity`, `BankStatementLineJpaEntity`, `InternalCashBoxJpaEntity`, `CashMovementJpaEntity`, `DailyClosingJpaEntity`; one `BankIntegrationPort` implementation per bank; a CNAB 240/400 parser/generator; an OFX/CSV parser.

## Cross-module dependencies

- **Consumes from `tax` (M2/M4)**: accounts receivable on invoicing; accounts payable on confirmed inbound receipt.
- **Consumes from `purchasing` (M6)**: payable generation on confirmed receipt.
- **Consumes from `sales` (M7)**: receivable generation on invoicing.
- **Consumes from `inventory` (M5)**: accounting entries from stock adjustments.
- **Provides to `masterdata`**: customer credit/delinquency status.
- **Provides to `reporting` (M9)**: DRE gerencial inputs, aging, cash flow.

## Notes

- The doc references "aprovação por alçada" here too (as in M6/M7); this spec assumes the same shared alçada configuration described in [m10-sistema.md](m10-sistema.md), not a finance-specific one.
- `AdjustReceivableForReturnUseCase` (uc-21) and `ConfirmBatchPaymentUseCase` (uc-22) were added after the initial split surfaced two gaps: nothing reverted a receivable when M7 registers a sales return, and the payable side had no settlement step symmetrical to the receivables' `ImportBankReturnUseCase` (uc-05) for its CNAB return. Both reuse the existing `Receivable`/`Payable`/`Settlement` domain model — no new aggregates. They're numbered 21/22 (appended) rather than renumbered into their §9.1/§9.2 groups, to avoid relinking every other ticket.
