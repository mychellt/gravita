# M2 — Fiscal: NFe — Implementation Tickets

Index of the standalone use-case tickets for [M2 — Fiscal: NFe](../m2-fiscal-nfe.md), each granular enough to plan and estimate independently. Phase references are the build phases from [11-ux-e-decisoes-tecnicas.md](../11-ux-e-decisoes-tecnicas.md#suggested-build-roadmap-doc-14).

| # | Ticket | Phase | Responsibility |
|---|---|---|---|
| 01 | [Issue NFe](uc-01-issue-nfe.md) | 2 — Fiscal Core | Create and queue an NFe, manual or from an approved sales order. |
| 02 | [Calculate Tax](uc-02-calculate-tax.md) | 1 — Fundação | Shared tax engine (ICMS/ST, IPI, PIS, COFINS, FCP) — used by every fiscal issuance use case across M2/M3/M4. |
| 03 | [Transmit NFe](uc-03-transmit-nfe.md) | 2 — Fiscal Core | Sign, submit to SEFAZ, handle contingency/backoff. |
| 04 | [Cancel NFe](uc-04-cancel-nfe.md) | 2 — Fiscal Core | Cancel within the legal window, with justification. |
| 05 | [Issue Correction Letter](uc-05-issue-correction-letter.md) | 2 — Fiscal Core | Register a CC-e event (max 20). |
| 06 | [Void Document Number Range](uc-06-void-document-number-range.md) | 2 — Fiscal Core | Register an `Inutilização`. |
| 07 | [Manifest Inbound NFe](uc-07-manifest-inbound-nfe.md) | 2 — Fiscal Core | Record recipient manifestation for an inbound NFe. |
| 08 | [Import Supplier NFe XML](uc-08-import-supplier-nfe-xml.md) | 2 — Fiscal Core | Parse a supplier's XML into an `InboundNfe`, ready for conference. |
| 09 | [Enter Inbound NFe Manually](uc-09-enter-inbound-nfe-manually.md) | 2 — Fiscal Core | Manual entry when no XML is available. |
| 10 | [Confirm Inbound NFe Receipt](uc-10-confirm-inbound-nfe-receipt.md) | 2 — Fiscal Core | Confirms conference, triggers stock entry and payable generation. |
| 11 | [Generate SPED Fiscal](uc-11-generate-sped-fiscal.md) | 7 — Visibilidade | EFD ICMS/IPI TXT generation per period. |
| 12 | [Generate SPED Contribuições](uc-12-generate-sped-contribuicoes.md) | 7 — Visibilidade | EFD PIS/COFINS TXT generation per period. |
| 13 | [Generate Livros Fiscais](uc-13-generate-livros-fiscais.md) | 7 — Visibilidade | Entry/Exit/ICMS Assessment books per period. |
| 14 | [Export Accounting Entries](uc-14-export-accounting-entries.md) | 7 — Visibilidade | CSV/TXT export in the configured accounting format. |

## Reading order for Phase 2 (Fiscal Core)

Tickets 01, 03, 04, 05, 06 form the outbound NFe lifecycle; 07, 08, 09, 10 form the inbound (purchase) NFe lifecycle. Ticket 02 (tax engine) is a Phase 1 prerequisite for both, built ahead of this module alongside M1/M10 because M3's PDV and M4's NFSe also depend on it from day one. Tickets 11–14 (SPED/books/accounting export) don't ship until Phase 7, alongside M9.
