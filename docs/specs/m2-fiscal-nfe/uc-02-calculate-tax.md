# UC-M2-02 — Calculate Tax (`CalculateTaxUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§3.1: "Automatic calculation: ICMS (normal and ST), IPI, PIS, COFINS, FCP; manual override with justification." Doc §13, "Motor fiscal único": one single service calculates ICMS, PIS, COFINS, IPI, ISS, called by every module — never duplicated.

## Description

Shared tax engine invoked synchronously by every fiscal-document issuance use case (this module's [UC-M2-01](uc-01-issue-nfe.md), M3's PDV sale, M4's NFSe emission) and by `sales`/`purchasing` for previews and inbound tax-credit computation. Precondition: the tax rate table (`NCM × UF × Regime × Operação → alíquotas`, doc §13) and the product's tax profile (M1) are populated. Given a set of item lines and operation context, returns a per-item tax breakdown; a manual override on any item requires a justification string and is recorded alongside the computed value.

## Port signature

```java
public interface CalculateTaxUseCase {
    TaxCalculationResult execute(CalculateTaxCommand command);
}
```

`CalculateTaxCommand` fields: `items: [{productRef, quantity, unitPrice}]`, `originState`, `destinationState`, `taxRegime`, `operationType`, `overrides: [{itemIndex, tax, value, justification}]` (optional). Returns `TaxCalculationResult`: per-item ICMS (normal/ST), IPI, PIS, COFINS, FCP breakdown plus totals.

## Outbound ports required

- `TaxRuleTableRepositoryPort` — reads the parameterized `NCM × UF × Regime × Operação` rate table. *Not named in the module spec's outbound-ports table; inferred here from doc §13's "tabela de tributação" decision.*

## REST endpoint

None dedicated. This is an internal collaborator port consumed by other use cases' controllers (`POST /api/nfe`, the M3 PDV sale endpoint, the M4 NFSe endpoint); it has no standalone REST surface in the module spec's adapter table.

## Domain entities touched

- Produces the tax-breakdown value object embedded in `NfeItem` (M2), the PDV sale item (M3) and the NFSe service line (M4) — it owns no aggregate of its own.

## Acceptance criteria

- [ ] Computes ICMS (normal and ST), IPI, PIS, COFINS and FCP per item, driven entirely by parameterized rate-table data — zero code changes for a new NCM/UF/regime/operation combination (doc §13).
- [ ] Produces identical results regardless of caller (NFe, NFCe, NFSe, sales/purchasing preview) — one implementation, no per-module duplication.
- [ ] A manual override is only accepted together with a non-empty justification; the original computed value is retained for audit.
- [ ] Works across all three tax regimes (Simples Nacional, Lucro Presumido, Lucro Real) with no code branching per regime — the regime only selects which rate-table rows apply.

## Dependencies

- **Depends on:** M1's tax rate table and product tax-profile data.
- **Blocks:** [UC-M2-01](uc-01-issue-nfe.md) and every other fiscal-document issuance use case across M2/M3/M4; M6's inbound tax-credit calculation; M7's order-level tax preview.

## Notes

- This use case is documented under M2 for narrative convenience, but per the roadmap (doc §14) it is built in **Phase 1 — Fundação**, alongside M1 and M10, because M3's PDV sale and M4's NFSe emission both depend on it from day one — it cannot wait for Phase 2.
- M3's NFCe issuance and M4's NFSe issuance both reuse this exact use case rather than each defining their own `CalculateTaxUseCase` — there is only one implementation in `br.gravita.tax`, referenced from all three module specs.
