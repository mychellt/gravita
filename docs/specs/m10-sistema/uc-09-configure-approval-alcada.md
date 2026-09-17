# UC-M10-09 — Configure Approval Alçada (`ConfigureApprovalAlcadaUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§11.1 (granular configuration, referenced by §7 "aprovação: alçada configurável por valor", §8.1 "aprovação por alçada de valor de pedido ou percentual de desconto", §9.2 "aprovação por alçada de valor") — a shared, value-threshold-based approval configuration consumed by `purchasing`, `sales` and `finance`.

## Description

An administrator sets, per module, the value (or discount-percentage) threshold above which an action requires approval, and which profile can approve it. Precondition: requester holds `system → alcadas → edit`. Postcondition: `purchasing`'s `ApprovePurchaseOrderUseCase`, `sales`'s `ApproveSalesOrderUseCase` and `finance`'s `ApprovePayableUseCase` resolve their approval requirement against this single, centralized configuration rather than each maintaining its own.

## Port signature

```java
public interface ConfigureApprovalAlcadaUseCase {
    void execute(ConfigureApprovalAlcadaCommand command);
}
```

`ConfigureApprovalAlcadaCommand`: `module` (`purchasing`, `sales`, `finance`), `thresholdValue` and/or `thresholdDiscountPercent`, `approverProfile: ProfileRef`.

## Outbound ports required

- `ApprovalAlcadaRepositoryPort`
- `ProfileRepositoryPort`

## REST endpoint

`PUT /api/system/alcadas/{module}`

## Domain entities touched

- `ApprovalAlcada`
- `Profile` (referenced, not mutated)

## Acceptance criteria

- [ ] Each module has exactly one active `ApprovalAlcada` configuration at a time.
- [ ] `approverProfile` must reference an existing profile (standard or custom).
- [ ] Updating a threshold takes effect on the next approval check — no need to touch `purchasing`/`sales`/`finance` code.

## Dependencies

- **Depends on:** UC-M10-03/UC-M10-04 (a `Profile` must exist to reference as approver).
- **Blocks:** M6's `ApprovePurchaseOrderUseCase`, M7's `ApproveSalesOrderUseCase`, M8's `ApprovePayableUseCase` — none of them can resolve an approval requirement without this configuration in place.
