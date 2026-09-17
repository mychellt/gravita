# UC-M1-14 — Manage Auxiliary Table (`ManageAuxiliaryTableUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.6 Tabelas Auxiliares: payment terms; payment methods; cost-center tree; chart-of-accounts tree; IBGE municipality table (pre-loaded); interstate ICMS rate table (updated via import).

## Description

CRUD for the six auxiliary reference tables that other modules read but don't own. Four are user-managed (`PaymentTerm`, `PaymentMethod`, `CostCenter`, `ChartOfAccounts`); two are populated by bulk import rather than per-record CRUD (`IbgeMunicipality`, pre-loaded at setup; `InterstateIcmsRate`, updated manually via import) — see Acceptance criteria. Precondition: none. Postcondition: the auxiliary table reflects the change.

## Port signature

```java
public interface ManageAuxiliaryTableUseCase {
    void execute(ManageAuxiliaryTableCommand command);
}
```

`ManageAuxiliaryTableCommand`: `tableType: {PAYMENT_TERM, PAYMENT_METHOD, COST_CENTER, CHART_OF_ACCOUNTS, IBGE_MUNICIPALITY, INTERSTATE_ICMS_RATE}`, `operation: {CREATE, UPDATE, DELETE, IMPORT}`, `payload` (shape depends on `tableType`).

## Outbound ports required

- A repository port per table (not separately named in the module spec's outbound-ports table; treated here as sub-ports of the same persistence concern, e.g. `PaymentTermRepositoryPort`, `CostCenterRepositoryPort`, etc.)

## REST endpoint

`GET/POST/PATCH /api/auxiliary/{payment-terms|payment-methods|cost-centers|chart-of-accounts}` for the four CRUD tables. IBGE municipalities and interstate ICMS rates are not listed with their own REST CRUD routes in the module spec — they're populated by import (see Acceptance criteria); expose them as an import endpoint (e.g. `POST /api/auxiliary/ibge-municipalities/import`, `POST /api/auxiliary/interstate-icms/import`) rather than inventing per-record CRUD the source doc doesn't describe.

## Domain entities touched

- `PaymentTerm`, `PaymentMethod`, `CostCenter`, `ChartOfAccounts`, `IbgeMunicipality`, `InterstateIcmsRate`

## Acceptance criteria

- [ ] `CostCenter` and `ChartOfAccounts` support a hierarchical tree structure (parent/child), not a flat list.
- [ ] `PaymentTerm` supports a free number of installments and configurable intervals (not fixed to 30/60/90).
- [ ] `IbgeMunicipality` ships pre-loaded (seed data) rather than requiring manual entry.
- [ ] `InterstateIcmsRate` is updated exclusively via import (doc: "atualização manual via import") — this ticket doesn't need a per-row edit form for it.
- [ ] Deleting a `CostCenter`/`ChartOfAccounts` node in use by `finance` (M8) is rejected, not cascaded.

## Dependencies

- **Depends on:** None.
- **Blocks:** `sales` (M7, payment terms/methods), `finance` (M8, cost centers, chart of accounts), and `tax` (M2/M4, IBGE municipalities, interstate ICMS rates).
