# UX Guidelines, Technical Decisions & Roadmap

Source: ERP MVP doc §12–§14. These apply across every module (M1–M10), so they live in one shared file rather than being repeated in each module spec.

## Global interface rules (doc §12.1)

User experience is not an interface detail — it carries the same weight as a business rule. Non-negotiable usability standards:

- Maximum 3 clicks for any day-to-day operation, from the main screen.
- No operation screen shows more than 7 main fields simultaneously visible — advanced fields go in collapsible tabs.
- Error messages are always in clear Portuguese and state exactly what the user must do to fix them.
- Every search field responds in real time (300ms debounce); it never requires pressing Enter to see results.
- Non-blocking alerts appear as a toast (3s); alerts that require action appear as a fixed banner — never as a modal popup in the middle of a sales flow.
- The system never loses typed data — leaving a screen without saving offers automatic recovery.
- Dark mode is available and respected as the OS preference.

**Implementation implication**: these are frontend-wide constraints, not per-module concerns. They should be enforced by shared UI components (search input with built-in debounce, toast/banner primitives, autosave/recovery) rather than re-implemented per screen.

## Critical flows (doc §12.2)

### Fluxo 1 — Venda no PDV (goal: < 30 seconds)

1. Operator logs into the register (biometrics or 4-digit PIN) — the sale screen is already open.
2. Scans or types a barcode — the product is added to the cart instantly.
3. Repeats step 2 for each item.
4. Clicks "Finalizar" — the payment panel opens in the same screen.
5. Selects payment method and amount — change is calculated automatically.
6. Confirms — NFC-e is issued, DANFE is printed or sent via WhatsApp.

Maps to M3 ([m3-fiscal-nfce-pdv.md](m3-fiscal-nfce-pdv.md)): `RegisterNfceSaleUseCase` + `IssueNfceUseCase`.

### Fluxo 2 — Emissão de NFe a partir de Pedido (goal: < 2 minutes)

1. Accesses the approved order in the order listing.
2. Clicks "Faturar" — order data is auto-filled into the NF.
3. Reviews and adjusts freight/transport if needed.
4. Clicks "Emitir NF-e" — signature and SEFAZ transmission are automatic.
5. NF authorized: DANFE is generated and e-mailed to the recipient automatically.

Maps to M7 → M2 ([m7-vendas-crm.md](m7-vendas-crm.md) `InvoiceSalesOrderUseCase` → [m2-fiscal-nfe.md](m2-fiscal-nfe.md) `IssueNfeUseCase`/`TransmitNfeUseCase`).

### Fluxo 3 — Recebimento de Compra (goal: < 5 minutes)

1. Opens the open purchase order.
2. Imports the supplier's NF-e XML — fields are auto-filled.
3. Checks divergences between order and NF (quantities, values).
4. Confirms receipt — stock is updated and accounts payable are generated.

Maps to M6 ([m6-compras.md](m6-compras.md)): `ReceivePurchaseOrderUseCase` + `ImportSupplierNfeAtReceivingUseCase` + `ConfirmPurchaseReceiptUseCase`.

## Essential technical decisions for the MVP (doc §13)

Architecture decisions that impact the MVP's quality and delivery speed. To be reviewed by the tech team before development starts on the relevant module.

- **Motor fiscal único**: one single service calculates ICMS, PIS, COFINS, IPI, ISS, called by every module. Never duplicate tax logic. → `br.gravita.tax`'s `CalculateTaxUseCase`, referenced from [m2-fiscal-nfe.md](m2-fiscal-nfe.md), [m3-fiscal-nfce-pdv.md](m3-fiscal-nfce-pdv.md), [m4-fiscal-nfse.md](m4-fiscal-nfse.md).
- **Tabela de tributação**: fiscal rules are parameterized in the database (`NCM × UF × Regime × Operação → alíquotas`). Zero code for new fiscal scenarios.
- **Fila de transmissão**: NFe, NFCe and NFSe are queued before reaching SEFAZ. Automatic retry with exponential backoff. Never block the UI waiting for a SEFAZ response. → `TransmissionQueuePort`, shared across M2/M3/M4.
- **Numeração de séries**: centralized control with optimistic locking, to avoid duplication in multi-user/multi-branch scenarios. → `masterdata`'s `AllocateDocumentNumberUseCase` ([m1-cadastros-base.md](m1-cadastros-base.md)).
- **Adaptador de NFSe**: a single interface for NFSe issuance; each municipal standard is an independent adapter. Adding a municipality means implementing the adapter, not touching the core. → `IssueNfsePort` ([m4-fiscal-nfse.md](m4-fiscal-nfse.md)).
- **Armazenamento de XML**: fiscal XMLs are stored in object storage (S3 or equivalent), not in the database. The database only holds a reference; the file lives in storage. → `XmlObjectStoragePort`.
- **Multiempresa**: tenant isolated by CNPJ in the same database, with a separate schema. Activated by a flag, no migration required.
- **Auditoria por trigger**: the audit trail is written by a database trigger — never dependent on application code, to guarantee completeness. → `system`'s `AuditTrailEntry` ([m10-sistema.md](m10-sistema.md)).

## Explicitly out of scope for the MVP (doc §13.1)

| Item | Context / Reason |
|---|---|
| RH / Folha de pagamento | Independent module; post-MVP integration via data export. |
| Contabilidade completa | The MVP exports entries; full bookkeeping stays in the accountant's system. |
| E-commerce próprio | Integration with existing platforms (Shopify, VTEX) via API — not building a storefront. |
| BI avançado / OLAP | The executive dashboard covers the MVP; an analytical cube is post-MVP evolution. |
| App mobile nativo | Responsive web covers MVP field operations; a native app is on the roadmap. |
| Integração com ERP SAP | Out of scope for a generic retail MVP. |

These are registered explicitly to prevent scope creep during development — if a request maps to one of these rows, it belongs in a future phase, not the MVP.

## Suggested build roadmap (doc §14)

Sequence chosen to deliver incremental value and allow real testing from the first sprint. **Each phase ends with a functional, testable deliverable — don't start the next phase without user validation of the current one.**

| Phase | Scope | Modules |
|---|---|---|
| 1 — Fundação | Master data, users/permissions, tax engine, tax rate table | M1, M10, tax engine core |
| 2 — Fiscal Core | NFe issuance/transmission/DANFE, NFCe + PDV (online), digital certificate | M2, M3 |
| 3 — Operação | Stock, purchasing, supplier NF-e entry | M5, M6 |
| 4 — Comercial | Sales & CRM: quote → order → invoicing | M7 |
| 5 — Financeiro | Full financial + bank integration (boleto, PIX, CNAB) | M8 |
| 6 — Serviços | NFSe (ABRASF + Nacional) + withholdings | M4 |
| 7 — Visibilidade | Dashboard, reports, SPED export | M9 |
| 8 — Robustez | Offline PDV (NFCe contingency), full audit, backup, load testing | M3 hardening, M10 hardening |

This phase order is reflected in [00-overview.md](00-overview.md#roadmap-doc-14); this file carries the reasoning and the explicit non-goals that justify why later phases (4/4 CRM automation, offline contingency, OLAP) are sequenced where they are rather than pulled forward.

### Ticket-level breakdown

Every use case across M1–M10 has its own implementation ticket (`<module>/uc-<NN>-<slug>.md`), each tagged with the roadmap phase above — see [00-overview.md § Implementation tickets](00-overview.md#implementation-tickets-per-use-case) for the full phase-by-phase ticket count. Module boundaries and phase boundaries mostly line up, but not always:

- M2's `CalculateTaxUseCase` ships in **Phase 1**, not Phase 2, because M3 and M4 both need the tax engine from day one.
- M2's SPED/livros-fiscais/accounting-export use cases ship in **Phase 7** with M9 reporting, not in Phase 2 with the rest of NFe issuance — SPED is a periodic obligations export, not part of the sale-to-DANFE critical path.
- M3's contingency/offline use cases (`SyncContingencySalesUseCase`, `VoidUntransmittedNumberingUseCase`) ship in **Phase 8** — the online PDV flow ships in Phase 2, offline hardening comes later.
- M10's audit-trail query, SEFAZ monitoring, backup and alerting use cases ship in **Phase 8** — user/permission management (needed by every other phase) ships in Phase 1, but the admin-facing observability tooling is hardening.

Use the module folders' `README.md` indexes as the sprint-planning backlog: each lists its tickets in build order with phase tags, so a phase can be scoped as an explicit ticket list rather than "build module X."
