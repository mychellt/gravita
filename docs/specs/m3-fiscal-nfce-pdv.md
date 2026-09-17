# M3 — Fiscal: NFCe + PDV (Modelo 65)

Source: ERP MVP doc §4. Context package: `br.gravita.tax` (adapter: NFCe + point-of-sale).

## Purpose

The PDV is the most critical screen for the cashier operator. It's designed for speed: register opens in one click, a standard sale completes in under 30 seconds.

## Functional requirements

### 4.1 Frente de Caixa (Front Desk)

- Cash opening: initial change amount; operator linked to a physical register.
- Product search: barcode (scanner or typed), internal code, description — immediate result.
- Add to cart: numeric keypad for quantity; product added with no extra confirmation step.
- Discount: per item or total; the max-discount limit is enforced automatically.
- Payment methods: multiple within the same sale (e.g. R$50 cash + R$30 card); change calculated automatically.
- CPF na nota: optional field; entered by typing, QR code, or the registered customer's CPF.
- Sangria / Suprimento: cash withdrawal or deposit, with justification, recorded immediately.
- Cash closing: reconciliation by payment method; Z report printed or saved as PDF.
- Cancellation: last sale, or any sale of the day, cancellable with a supervisor password.
- Multiple registers: each with its own operator; consolidated at day closing.

### 4.2 Transmissão NFCe

- Online mode: NFC-e issued and authorized in real time before printing; average time < 3s.
- Contingency mode: offline sale numbered with a pending flag; automatic sync on reconnect.
- DANFE NFC-e: printed on a non-fiscal 80mm/58mm printer; valid QR code; sendable via WhatsApp/e-mail.
- Cancellation: up to 30 minutes after issuance (minimum deadline); the state deadline applies when longer.
- Inutilização: numbering that was never transmitted is auto-voided at day end.

### 4.3 Regras de UX do PDV

- The PDV must be operable with the numeric keypad alone.
- No sale flow requires a screen change — everything happens in the same window.
- Out-of-stock or invalid-discount alerts appear as non-blocking toasts, never a blocking modal.
- The "Finalizar Venda" button is the largest visual element on the screen, always visible.
- Touch mode is available for tablets without a physical keyboard.

## Domain model

- **PosSession** (aggregate root) — `registerId`, `operatorId`, `openingChangeAmount: Money`, `openedAt`, `closedAt`, `status: {OPEN, CLOSED}`. Invariant: only one open session per physical register at a time.
- **NfceSale** (aggregate root) — `session: PosSessionRef`, `items: [SaleItem]`, `discounts`, `payments: [Payment]` (multiple methods per sale), `changeGiven: Money`, `customerCpf` (optional), `contingencyMode: boolean`, `status: {DRAFT, AUTHORIZED, PENDING_SYNC, CANCELLED, VOIDED}`. Reuses the same tax-calculation and transmission-queue collaborators as M2. Invariant: total payments must cover the sale total; `changeGiven` is a derived value, never entered directly.
- **CashMovement** — `type: {SANGRIA, SUPRIMENTO}`, `amount: Money`, `justification`, `timestamp`, `session: PosSessionRef`.
- **CashClosingReport (Relatório Z)** — per session: totals by payment method, opening/closing amounts, cash movements, sale count.

## Use cases (`application.port.in`)

Each use case below has a standalone implementation ticket under [`m3-fiscal-nfce-pdv/`](m3-fiscal-nfce-pdv/README.md), ordered as the build sequence.

| Use case | Responsibility |
|---|---|
| [`OpenPosSessionUseCase`](m3-fiscal-nfce-pdv/uc-01-open-pos-session.md) | Register open, with operator and initial change amount. |
| [`SearchProductForSaleUseCase`](m3-fiscal-nfce-pdv/uc-02-search-product-for-sale.md) | Barcode/code/description lookup, real-time. |
| [`RegisterNfceSaleUseCase`](m3-fiscal-nfce-pdv/uc-03-register-nfce-sale.md) | Build the cart, apply discounts, take payments, compute change. |
| [`IssueNfceUseCase`](m3-fiscal-nfce-pdv/uc-04-issue-nfce.md) | Reuses `CalculateTaxUseCase` (M2); issues online or queues in contingency. |
| [`RecordCashMovementUseCase`](m3-fiscal-nfce-pdv/uc-05-record-cash-movement.md) | Sangria/Suprimento with mandatory justification. |
| [`ClosePosSessionUseCase`](m3-fiscal-nfce-pdv/uc-06-close-pos-session.md) | Reconciliation by payment method and Z-report generation. |
| [`CancelNfceUseCase`](m3-fiscal-nfce-pdv/uc-07-cancel-nfce.md) | Cancel last sale or a specific sale of the day, requires supervisor password. |
| [`SyncContingencySalesUseCase`](m3-fiscal-nfce-pdv/uc-08-sync-contingency-sales.md) | Background sync of pending sales once connectivity returns. |
| [`VoidUntransmittedNumberingUseCase`](m3-fiscal-nfce-pdv/uc-09-void-untransmitted-numbering.md) | End-of-day auto-void for numbering never sent. |

## Outbound ports (`application.port.out`)

| Port | Purpose |
|---|---|
| `NfceRepositoryPort`, `PosSessionRepositoryPort`, `CashMovementRepositoryPort` | Persistence. |
| `TransmissionQueuePort`, `SubmitToSefazPort` (shared with M2) | Online/contingency transmission. |
| `PrintNonFiscalReceiptPort` | 80mm/58mm printer output for DANFE NFC-e and the Z report. |
| `SendFiscalDocumentByWhatsAppPort` (extends the M10 WhatsApp integration) | DANFE NFC-e delivery on request. |
| `SupervisorAuthorizationPort` | Validates the supervisor password for cancellations. |

## Adapters

### Inbound (`adapter.in.web`)

| Method & path | Use case |
|---|---|
| `POST /api/pdv/sessions` / `POST /api/pdv/sessions/{id}/close` | Open/close session |
| `GET /api/pdv/products/search?q=` | `SearchProductForSaleUseCase` |
| `POST /api/pdv/sales` | `RegisterNfceSaleUseCase` + `IssueNfceUseCase` |
| `POST /api/pdv/sales/{id}/cancel` | `CancelNfceUseCase` |
| `POST /api/pdv/cash-movements` | `RecordCashMovementUseCase` |
| `GET /api/pdv/sessions/{id}/z-report` | Z-report retrieval |

The PDV frontend is a single-window, keypad-driven UI (see [11-ux-e-decisoes-tecnicas.md](11-ux-e-decisoes-tecnicas.md)); it is a consumer of this REST surface, not a separate backend concern.

### Outbound (`adapter.out.persistence` / integrations)

`NfceSaleJpaEntity`, `PosSessionJpaEntity`, `CashMovementJpaEntity`; the same SEFAZ client adapter as M2; a local/network print adapter for non-fiscal receipts; the WhatsApp Business API adapter (shared with M10).

## Cross-module dependencies

- **Shares with `M2`**: `CalculateTaxUseCase`, `TransmissionQueuePort`/`SubmitToSefazPort`, numbering allocation.
- **Consumes from `masterdata`**: product, price table, customer lookup by CPF.
- **Provides to `inventory`**: stock exit per confirmed sale.
- **Provides to `finance`**: cash reconciliation totals feed `Caixa interno` (M8 §9.3).

## Notes

- The 30-second sale-time and <3s authorization targets are UX/performance budgets from the doc, not enforced by a specific mechanism here; they should become explicit performance tests once the flow is implemented (see Fluxo 1 in [11-ux-e-decisoes-tecnicas.md](11-ux-e-decisoes-tecnicas.md)).
