# UC-M8-12 — Approve Payable (`ApprovePayableUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.2 Contas a Pagar — "Aprovação: alçada por valor; aprovação remota por e-mail ou app."

## Description

Approves an open `Payable` against the configured approval alçada (M10's `ApprovalAlcadaRepositoryPort`) before it becomes eligible for payment. The approver acts remotely, via app or e-mail.

## Port signature

```java
public interface ApprovePayableUseCase {
    Payable execute(ApprovePayableCommand command);
}
```

`ApprovePayableCommand`: `payableId`, `approvedBy`. Returns the updated `Payable` (`status = APPROVED`).

## Outbound ports required

- `PayableRepositoryPort`
- `ApprovalAlcadaRepositoryPort` (from `system`, M10)

## REST endpoint

`POST /api/finance/payables/{id}/approve`

## Domain entities touched

- `Payable`

## Acceptance criteria

- [ ] A payable can only be approved if its value is within the approver's configured alçada.
- [ ] Approval moves `status` from `OPEN` to `APPROVED`.
- [ ] `approvedBy` is recorded on the payable.

## Dependencies

- **Depends on:** UC-M8-10 or UC-M8-11; M10's alçada configuration (`ConfigureApprovalAlcadaUseCase`).
- **Blocks:** UC-M8-13, UC-M8-14 (only approved payables can be paid).
