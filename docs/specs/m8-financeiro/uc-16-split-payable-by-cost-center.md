# UC-M8-16 — Split Payable by Cost Center (`SplitPayableByCostCenterUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.2 Contas a Pagar — "Centro de custo: rateio de despesa entre centros de custo com percentuais configuráveis."

## Description

Splits a payable's amount across one or more cost centers by configurable percentage, for expense reporting and DRE purposes.

## Port signature

```java
public interface SplitPayableByCostCenterUseCase {
    Payable execute(SplitPayableByCostCenterCommand command);
}
```

`SplitPayableByCostCenterCommand`: `payableId`, `split: [{costCenter, percent}]`. Returns the updated `Payable` with `costCenterSplit` set.

## Outbound ports required

- `PayableRepositoryPort`

## REST endpoint

Not listed separately in the module spec's adapter table — treated as part of the payable create/edit payload (`POST /api/finance/payables`, and a `PATCH` not yet spec'd).

## Domain entities touched

- `Payable`

## Acceptance criteria

- [ ] Percentages across all cost centers in the split sum to 100%.
- [ ] The split is available to M9's DRE gerencial report by cost center.

## Dependencies

- **Depends on:** UC-M8-10 or UC-M8-11.
- **Blocks:** —

## Notes

- Flag for the team: confirm whether this needs its own endpoint/use case, or should be folded into the create/update payable flow — the source doc describes it as a field of the payable, not a separate action.
