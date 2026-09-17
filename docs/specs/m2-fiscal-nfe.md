# M2 — Fiscal: NFe (Modelo 55)

Source: ERP MVP doc §3. Context package: `br.gravita.tax` (adapter: NFe).

## Purpose

The NFe covers B2B sales, inter-branch transfers, returns, shipments and any other operation requiring an A4 DANFE. It is the fiscal core of the system, and shares its tax engine, transmission queue and numbering with M3 and M4.

## Functional requirements

### 3.1 Emissão (Issuance)

- Issuance origin: manual (direct entry) or automatic from an approved sales order.
- Operation nature: free CFOP registration with automatic mapping by type (sale, return, shipment, …).
- Recipient: search by CPF, CNPJ or name; IE validation and taxpayer indicator.
- Items: search by barcode, internal code or description; variant grids and kits supported.
- Automatic calculation: ICMS (normal and ST), IPI, PIS, COFINS, FCP; manual override requires a justification.
- Discount: per item and total, respecting the linked price table's max-discount rule.
- Freight and accessories: freight, insurance, other expenses — impact the tax base.
- Transport: modality (CIF/FOB), carrier, volume, gross/net weight, RNTRC.
- Reference: access key of a prior NF, for returns and complementary notes.
- Additional info: free field for fiscal complements and data of interest to the tax authority.

### 3.2 Transmissão e Ciclo de Vida (Transmission & Lifecycle)

- Signature: automatic with the registered A1 certificate — no user interaction.
- SEFAZ submission: asynchronous batch with polling; retry queue on timeout.
- Contingency: SVC-AN / SVC-RS auto-activated when SEFAZ-UF is unavailable.
- DANFE: generated as PDF — portrait (default) or landscape; company logo; barcode.
- Send to recipient: XML + DANFE by automatic e-mail on authorization; manual resend available.
- Cancellation: within the legal deadline (up to 24h, or the state limit); justification required.
- CC-e: Carta de Correção Eletrônica for non-tax data; limited to 20 events.
- Inutilização: unused numbering voided with justification; the record is immutable.
- Manifestação do destinatário: confirmation, unknown or operation-not-performed, for inbound NFe.
- Storage: XML and DANFE kept for a minimum of 5 years; download available to the user.

### 3.3 Entradas (NF-e de Compra) (Inbound / Purchase NFe)

- XML import: upload the supplier's XML file; fields are auto-filled.
- Manual entry: type the access key or enter data manually for suppliers without XML.
- Conference: compares what was ordered, what physically arrived, and what's on the NF.
- Automatic stock: stock entry generated immediately after the receipt is confirmed.
- Accounts payable: titles auto-generated from the purchase NF's payment terms.
- Taxes: ICMS, PIS and COFINS credit calculated per the applicable tax regime.

### 3.4 SPED e Obrigações Acessórias

- SPED Fiscal: EFD ICMS/IPI TXT file generation; mandatory record validation.
- SPED Contribuições: EFD PIS/COFINS file generation; assessed per period.
- Livros fiscais: Entry, Exit and ICMS Assessment books generated per period.
- Relatório de tributos: ICMS, IPI, PIS, COFINS summary per period, for the accountant.
- Exportação contábil: entries exported in CSV/TXT in the configured accounting system's format.

## Domain model

- **NfeDocument** (aggregate root) — `accessKey`, `series`, `number`, `naturezaOperacao`, `cfop`, `issuer: CompanyRef`, `recipient: PersonRef`, `items: [NfeItem]`, `freight`, `insurance`, `otherExpenses`, `transport{modality, carrier, volume, grossWeight, netWeight, rntrc}`, `referencedAccessKey` (for returns/complementary notes), `additionalInfo`, `taxTotals` (computed by the tax engine), `status: {DRAFT, QUEUED, SENT, AUTHORIZED, REJECTED, CANCELLED, VOIDED}`, `protocol`, timestamps. Invariant: `status` transitions follow SEFAZ's lifecycle; only `AUTHORIZED` notes within the legal cancellation window can move to `CANCELLED`.
- **NfeItem** — product reference, quantity, unit price, discount, item-level tax breakdown (result of `CalculateTaxUseCase`), manual-override flag + justification.
- **CorrectionLetter (CC-e)** — sequence number (≤ 20 per document), text, protocol, timestamp.
- **VoidedNumberRange** — series, start/end number, justification, timestamp — records `Inutilização`.
- **InboundManifestation** — access key, `type: {CONFIRMED, UNKNOWN, OPERATION_NOT_PERFORMED}`, timestamp.
- **InboundNfe** — a purchase-side NFe: parsed/imported XML, conference result (ordered vs. received vs. billed quantities/values), generated stock entry and payable references.
- **TransmissionQueueEntry** — shared across M2/M3/M4 (doc §13): document ref, `attempt count`, `nextRetryAt` (exponential backoff), `contingencyMode: boolean`.

## Use cases (`application.port.in`)

Each use case below has a standalone implementation ticket under [`m2-fiscal-nfe/`](m2-fiscal-nfe/README.md), for granular phase planning. Build-phase placement doesn't always match this module's own grouping: the shared tax engine ships in Phase 1 (M3/M4 depend on it from day one), and SPED/books/accounting export ship in Phase 7 alongside M9, not in Phase 2's fiscal core — see each ticket.

| Use case | Responsibility |
|---|---|
| [`IssueNfeUseCase`](m2-fiscal-nfe/uc-01-issue-nfe.md) | Create and queue an NFe, manual or from an approved sales order. |
| [`CalculateTaxUseCase`](m2-fiscal-nfe/uc-02-calculate-tax.md) | Shared tax engine (ICMS/ST, IPI, PIS, COFINS, FCP) — invoked by this and every other issuing use case, and by `sales`/`purchasing`. |
| [`TransmitNfeUseCase`](m2-fiscal-nfe/uc-03-transmit-nfe.md) | Sign, submit to SEFAZ, handle contingency/backoff (queue worker). |
| [`CancelNfeUseCase`](m2-fiscal-nfe/uc-04-cancel-nfe.md) | Cancel within the legal window, with justification. |
| [`IssueCorrectionLetterUseCase`](m2-fiscal-nfe/uc-05-issue-correction-letter.md) | Register a CC-e event (max 20). |
| [`VoidDocumentNumberRangeUseCase`](m2-fiscal-nfe/uc-06-void-document-number-range.md) | Register an `Inutilização`. |
| [`ManifestInboundNfeUseCase`](m2-fiscal-nfe/uc-07-manifest-inbound-nfe.md) | Record recipient manifestation for an inbound NFe. |
| [`ImportSupplierNfeXmlUseCase`](m2-fiscal-nfe/uc-08-import-supplier-nfe-xml.md) | Parse a supplier's XML into an `InboundNfe`, ready for conference. |
| [`EnterInboundNfeManuallyUseCase`](m2-fiscal-nfe/uc-09-enter-inbound-nfe-manually.md) | Manual entry when no XML is available. |
| [`ConfirmInboundNfeReceiptUseCase`](m2-fiscal-nfe/uc-10-confirm-inbound-nfe-receipt.md) | Confirms conference, triggers stock entry (`inventory`) and payable generation (`finance`). |
| [`GenerateSpedFiscalUseCase`](m2-fiscal-nfe/uc-11-generate-sped-fiscal.md) / [`GenerateSpedContribuicoesUseCase`](m2-fiscal-nfe/uc-12-generate-sped-contribuicoes.md) | EFD TXT generation per period. |
| [`GenerateLivrosFiscaisUseCase`](m2-fiscal-nfe/uc-13-generate-livros-fiscais.md) | Entry/Exit/ICMS Assessment books per period. |
| [`ExportAccountingEntriesUseCase`](m2-fiscal-nfe/uc-14-export-accounting-entries.md) | CSV/TXT export in the configured accounting format. |

## Outbound ports (`application.port.out`)

| Port | Purpose |
|---|---|
| `NfeRepositoryPort`, `InboundNfeRepositoryPort` | Persistence for outbound/inbound NFe. |
| `TransmissionQueuePort` | Enqueue/dequeue with retry/backoff (shared M2/M3/M4). |
| `SubmitToSefazPort` | SEFAZ web-service integration, production and homologation, with SVC-AN/SVC-RS contingency. |
| `GenerateDanfePort` | PDF rendering, portrait/landscape. |
| `SendFiscalDocumentByEmailPort` | XML + DANFE delivery. |
| `XmlObjectStoragePort` | Stores XML/DANFE in object storage; the DB only holds a reference (doc §13). |
| `AllocateDocumentNumberUseCase` (from `masterdata`) | Numbering, called before queuing. |
| `NotifyStockEntryPort` (into `inventory`), `NotifyPayableGeneratedPort` (into `finance`) | Side effects of confirmed inbound receipt. |
| `VoidedNumberRangeRepositoryPort` | Persistence for `VoidedNumberRange` (Inutilização records). |
| `GenerateSpedFilePort` | EFD TXT generation, shared shape for SPED Fiscal and SPED Contribuições. |
| `GenerateFiscalBookPort` | PDF/TXT rendering for the Entry/Exit/ICMS Assessment books and tax summary — an aggregated period report, distinct from `GenerateDanfePort`'s single-document rendering. |
| `ExportAccountingFilePort` | CSV/TXT accounting export, per the company's configured format (M1/M10). |

## Adapters

### Inbound (`adapter.in.web`)

| Method & path | Use case |
|---|---|
| `POST /api/nfe` | `IssueNfeUseCase` |
| `POST /api/nfe/{id}/cancel` | `CancelNfeUseCase` |
| `POST /api/nfe/{id}/correction-letters` | `IssueCorrectionLetterUseCase` |
| `POST /api/nfe/void-range` | `VoidDocumentNumberRangeUseCase` |
| `POST /api/nfe/inbound/manifestation` | `ManifestInboundNfeUseCase` |
| `POST /api/nfe/inbound/import-xml` | `ImportSupplierNfeXmlUseCase` |
| `POST /api/nfe/inbound` | `EnterInboundNfeManuallyUseCase` |
| `POST /api/nfe/inbound/{id}/confirm-receipt` | `ConfirmInboundNfeReceiptUseCase` |
| `POST /api/sped/fiscal`, `POST /api/sped/contribuicoes` | SPED generation |
| `GET /api/livros-fiscais` | `GenerateLivrosFiscaisUseCase` |
| `POST /api/accounting/export` | `ExportAccountingEntriesUseCase` |
| `GET /api/nfe/{id}/danfe` | Download rendered DANFE |

### Outbound (`adapter.out.persistence` / integrations)

`NfeJpaEntity`, `NfeItemJpaEntity`, `InboundNfeJpaEntity`, `TransmissionQueueJpaEntity`; a SEFAZ SOAP/REST client adapter; an object-storage adapter (S3-compatible) for XML/DANFE; an e-mail adapter reused from the shared notification port.

## Cross-module dependencies

- **Consumes from `masterdata`**: company fiscal data, certificate, document series, customer/supplier, product tax profile.
- **Consumes from `sales`**: approved order data, for automatic issuance.
- **Provides to `inventory`**: stock entry on confirmed inbound receipt.
- **Provides to `finance`**: accounts payable on confirmed inbound receipt; accounts receivable trigger when invoicing a sales order (see M7).
- **Shared with `M3`/`M4`**: `CalculateTaxUseCase`, `TransmissionQueuePort`, numbering.

## Notes

- The functional doc doesn't name a specific SEFAZ SDK/library; `SubmitToSefazPort` is left as an integration boundary to be filled by whichever client library the team picks.
- `TransmissionQueueEntry` and `CalculateTaxUseCase` are modeled once here and referenced by M3/M4 rather than repeated, per doc §13's "motor fiscal único" and "fila de transmissão" decisions.
