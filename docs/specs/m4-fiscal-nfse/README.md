# M4 — Fiscal: NFSe — Implementation Tickets

Index of per-use-case implementation tickets for [../m4-fiscal-nfse.md](../m4-fiscal-nfse.md). Each ticket is a standalone, implementable unit derived from that module spec's functional requirements, domain model and use case table.

| # | Ticket | Phase | Responsibility |
|---|---|---|---|
| 01 | [Register Municipality Integration](uc-01-register-municipality-integration.md) | 6 — Serviços | Configure a municipality's webservice/standard/required fields. |
| 02 | [Issue RPS](uc-02-issue-rps.md) | 6 — Serviços | Create an internal RPS. |
| 03 | [Convert RPS to NFSe](uc-03-convert-rps-to-nfse.md) | 6 — Serviços | Batch or individual conversion to `NfseDocument`. |
| — | Calculate Tax (`CalculateTaxUseCase`) | 1 — Fundação | Owned by M2, reused here for ISS rate resolution — see [../m2-fiscal-nfe/uc-02-calculate-tax.md](../m2-fiscal-nfe/uc-02-calculate-tax.md). |
| 04 | [Transmit NFSe](uc-04-transmit-nfse.md) | 6 — Serviços | Submit via the standard-specific adapter, or fall back to guided manual upload. |
| 05 | [Cancel NFSe](uc-05-cancel-nfse.md) | 6 — Serviços | Cancellation via municipality webservice, with justification. |
| 06 | [Manage Discrimination Template](uc-06-manage-discrimination-template.md) | 6 — Serviços | CRUD for per-service-type templates. |

Suggested implementation order within Phase 6: 01 → 06 (templates are independent, can be done any time) → 02 → 03 → 04 → 05, since each of 02/03/04/05 builds on state produced by the previous one.
