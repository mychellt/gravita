# ERP MVP — Specs Overview

Source: [`docs/ERP MVP formato DOC.pdf`](../ERP%20MVP%20formato%20DOC.pdf) ("ERP MVP — Documentação Funcional Completa", v1.0, Varejo Genérico, Múltiplos Perfis Fiscais, PT-BR).

This directory restates that document as one spec per module (M1–M10), in English, and adds a technical mapping onto the hexagonal, package-by-feature architecture described in [`docs/ARCHITECTURE.md`](../ARCHITECTURE.md). The functional requirements are a faithful translation of the source PDF; the technical mapping (domain model, use cases, ports, endpoints) is a proposed design derived from it and is flagged wherever it goes beyond what the PDF states.

## Product philosophy

The MVP specifies a generic ERP for Brazilian retail, built to serve multiple segments and tax profiles from a single codebase — no per-client customization, no fragile optional modules.

## Non-negotiable principles

- **Fewer screens, more result**
  - Any day-to-day operation must be completable in at most 3 clicks from the main screen.
  - Sales, invoice issuance and payment receipt flows are never interrupted by unnecessary screens.
  - Advanced configuration lives in its own area — daily operation never surfaces it.
- **Single standard, zero per-client customization**
  - Every variable business rule (CFOP, CSOSN, rates, tax regime) is parameterized in registration data, not in code.
  - The system works for Simples Nacional, Lucro Presumido and Lucro Real with no code changes.
  - There is no "disabled module" in the MVP: if a client doesn't use NFSe, the menu simply doesn't show it — but the code is there.
- **Robust from day one**
  - Every fiscal emission has a retransmission queue with automatic retry and offline contingency.
  - No fiscal document can be physically deleted — only cancelled within the legal deadline.
  - Every user action is audited: who, what, when, previous value and new value.

## Module map

| # | Module | Spec | Tickets | Context package |
|---|--------|------|---------|------------------|
| M1 | Cadastros Base (Master Data) | [m1-cadastros-base.md](m1-cadastros-base.md) | [15](m1-cadastros-base/README.md) | `br.gravita.masterdata` |
| M2 | Fiscal — NFe | [m2-fiscal-nfe.md](m2-fiscal-nfe.md) | [14](m2-fiscal-nfe/README.md) | `br.gravita.tax` |
| M3 | Fiscal — NFCe + PDV | [m3-fiscal-nfce-pdv.md](m3-fiscal-nfce-pdv.md) | [9](m3-fiscal-nfce-pdv/README.md) | `br.gravita.tax` |
| M4 | Fiscal — NFSe | [m4-fiscal-nfse.md](m4-fiscal-nfse.md) | [6](m4-fiscal-nfse/README.md) | `br.gravita.tax` |
| M5 | Estoque (Inventory) | [m5-estoque.md](m5-estoque.md) | [11](m5-estoque/README.md) | `br.gravita.inventory` |
| M6 | Compras (Purchasing) | [m6-compras.md](m6-compras.md) | [9](m6-compras/README.md) | `br.gravita.purchasing` |
| M7 | Vendas & CRM | [m7-vendas-crm.md](m7-vendas-crm.md) | [16](m7-vendas-crm/README.md) | `br.gravita.sales` |
| M8 | Financeiro | [m8-financeiro.md](m8-financeiro.md) | [20](m8-financeiro/README.md) | `br.gravita.finance` |
| M9 | Relatórios & BI | [m9-relatorios-bi.md](m9-relatorios-bi.md) | [9](m9-relatorios-bi/README.md) | `br.gravita.reporting` |
| M10 | Configurações e Sistema | [m10-sistema.md](m10-sistema.md) | [13](m10-sistema/README.md) | `br.gravita.system` |

M2, M3 and M4 share the `tax` context: they are three inbound adapters (NFe, NFCe+PDV, NFSe) around one tax engine and one set of fiscal use cases (doc §13, "Motor fiscal único").

UX rules, cross-cutting technical decisions, explicit non-goals and the suggested build roadmap are in [11-ux-e-decisoes-tecnicas.md](11-ux-e-decisoes-tecnicas.md).

## How to read a module spec

Each file follows the same structure:

1. **Purpose** — one or two sentences on why the module exists.
2. **Functional requirements** — translated from the PDF, organized by the same subsections.
3. **Domain model** — aggregates, entities, value objects and their invariants (`domain.model`).
4. **Use cases** — inbound ports the module offers (`application.port.in`), each one linked to its own implementation ticket (see below).
5. **Outbound ports** — what the module needs from persistence and integrations (`application.port.out`).
6. **Adapters** — the REST surface (`adapter.in.web`) and the persistence/integration adapters (`adapter.out.persistence`).
7. **Cross-module dependencies** — what the module consumes from, or provides to, other contexts.
8. **Notes** — anything proposed or inferred beyond the source PDF, called out explicitly so it can be challenged in review.

## Implementation tickets (per use case)

Every use case listed in a module spec has its own standalone ticket file under `<module>/uc-<NN>-<slug>.md` (e.g. [`m1-cadastros-base/uc-01-register-company.md`](m1-cadastros-base/uc-01-register-company.md)), with a `README.md` index per module folder. A ticket is self-contained: functional requirement, description, port signature, outbound ports it calls, REST endpoint, domain entities touched, acceptance criteria, and its dependencies on other tickets/modules — small enough to be one implementation unit (roughly one PR).

Each ticket also carries a **roadmap phase** (doc §14), which doesn't always match its module's default phase — a handful of use cases ship earlier or later than the rest of their module:

| Phase | Modules / use cases | Tickets |
|---|---|---|
| 1 — Fundação | M1 (all) + M2's `CalculateTaxUseCase` (tax engine) + M10 users/permissions/alçada/integration-credentials | 25 |
| 2 — Fiscal Core | M2 issuance/transmission/inbound (all but tax engine and SPED) + M3 online PDV flow | 16 |
| 3 — Operação | M5 (all) + M6 (all) | 20 |
| 4 — Comercial | M7 (all) | 16 |
| 5 — Financeiro | M8 (all) | 20 |
| 6 — Serviços | M4 (all; its `CalculateTaxUseCase` row links to M2's Phase 1 ticket instead of duplicating it) | 6 |
| 7 — Visibilidade | M2's SPED/livros/accounting-export use cases + M9 (all) | 13 |
| 8 — Robustez | M3's contingency/void use cases + M10's audit-trail query, SEFAZ monitoring, backup, alerting | 6 |

That's 122 tickets total (M7 counts 16: use case 16, "Manage Follow-up Rule," was added after the initial split to give use case 12 a CRUD port to evaluate — see [m7-vendas-crm/README.md](m7-vendas-crm/README.md#patched-gaps)). The two exceptions worth remembering: M2's tax engine (`CalculateTaxUseCase`) is built in Phase 1 because M3 and M4 depend on it from day one, and M2's SPED/fiscal-books/accounting-export use cases ship in Phase 7 with reporting, not in Phase 2 with the rest of NFe.

## Technical mapping conventions

- Package-by-feature under `br.gravita.<context>`, per `ARCHITECTURE.md`: `domain/model`, `application/port/{in,out}`, `application/service`, `adapter/{in/web,out/persistence}`.
- `br.gravita.shared` holds cross-context value objects and the base exception. Today that's `Document` and `PersonType`. These specs also lean on `Money` and `Percentage` as shared value objects — proposed here since every fiscal, sales, purchasing and finance module needs them; not yet present in the shared kernel.
- Inbound port interfaces are suffixed `UseCase` (e.g. `RegisterCustomerUseCase`); their implementation lives in `application.service` and is annotated `@UseCase`.
- Outbound port interfaces are suffixed `Port` (e.g. `CustomerRepositoryPort`, `IssueNfePort`); persistence adapters are annotated `@PersistenceAdapter`.
- Tax calculation is centralized in `br.gravita.tax`: `CalculateTaxUseCase` is an inbound port of the `tax` context that `sales`, `purchasing` and `masterdata` call as a collaborator. No other context re-implements tax logic (doc §13).
- NFSe issuance is one port (`IssueNfsePort`) with one adapter per municipal standard (ABRASF, NFS-e Nacional, ISS.net/Betha…). Adding a municipality means adding an adapter, not touching the core (doc §13).

## Existing code note

The code currently under `br.gravita.core` / `br.gravita.adapters` (`CustomerDomain`, `PlanDomain`, `SubscriptionJpaEntity`, `PaymentJpaEntity`, …) implements Gravita's own SaaS billing — the platform charging its tenants for a subscription plan. It is a separate bounded context from every module in this spec set, predates the package-by-feature layout in `ARCHITECTURE.md`, and does not implement any of M1–M10. In particular, don't confuse the existing `CustomerDomain` (a Gravita subscriber) with the M1 "Clientes" entity spec'd in [m1-cadastros-base.md](m1-cadastros-base.md) (a tenant's own retail customer) — the names collide but the two are unrelated. `br.gravita.masterdata`, referenced by `ARCHITECTURE.md` as the "living reference" implementation, does not exist in the tree yet; these specs treat it as the target shape for new work, not as already-built code to imitate.

## Roadmap (doc §14)

Suggested build sequence — incremental value, real testing from sprint 1. Each phase ends with a functional, testable deliverable; don't start the next phase without user validation of the current one. The [ticket-level phase breakdown](#implementation-tickets-per-use-case) above is the granular version of this list — use it to plan a phase as a concrete set of tickets rather than a whole module at a time.

1. **Fundação** — M1 Cadastros Base + M10 Users & Permissions + tax engine + tax rate table.
2. **Fiscal Core** — M2 NFe (issuance, transmission, DANFE) + M3 NFCe + PDV (online) + digital certificate.
3. **Operação** — M5 Estoque + M6 Compras + supplier NF-e entry.
4. **Comercial** — M7 Vendas & CRM (quote → order → invoicing).
5. **Financeiro** — M8 full financial + bank integration (boleto, PIX, CNAB).
6. **Serviços** — M4 NFSe (ABRASF + Nacional) + withholdings.
7. **Visibilidade** — M9 dashboard + reports + SPED export.
8. **Robustez** — offline PDV (NFCe contingency) + full audit + backup + load testing.

## Explicitly out of scope for the MVP (doc §13.1)

RH/payroll, full bookkeeping, a proprietary e-commerce storefront, advanced BI/OLAP, a native mobile app, and SAP ERP integration. Details and rationale in [11-ux-e-decisoes-tecnicas.md](11-ux-e-decisoes-tecnicas.md).
