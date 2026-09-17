# UC-M6-05 — Approve Purchase Order (`ApprovePurchaseOrderUseCase`)

**Module:** M6 — Compras ([module spec](../m6-compras.md))
**Context package:** `br.gravita.purchasing`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

Aprovação: configurable approval alçada by value, with an approval workflow via app/e-mail.

## Description

When a `PurchaseOrder`'s value requires approval per the resolved `approvalAlcada`, the designated approver (per `system`'s alçada config) approves or rejects it via an app/e-mail notification. Receiving (UC-M6-06) cannot start until an order requiring approval has been approved.

## Port signature

```java
public interface ApprovePurchaseOrderUseCase {
    void execute(ApprovePurchaseOrderCommand command);
}
```

`ApprovePurchaseOrderCommand`: `orderId: PurchaseOrderId`, `approvedBy`, `decision: {APPROVE, REJECT}`.

## Outbound ports required

- `PurchaseOrderRepositoryPort`
- `NotifyApprovalWorkflowPort`

## REST endpoint

`POST /api/purchasing/orders/{id}/approve`

## Domain entities touched

- `PurchaseOrder` (`approval.approvedBy` set; rejection cancels the order)

## Acceptance criteria

- [ ] Approval is only requested for orders whose value exceeds the resolved `approvalAlcada` threshold.
- [ ] Orders below the threshold skip this step and are immediately receivable.
- [ ] A rejection transitions the order to `CANCELLED`.
- [ ] The approver is notified via app/e-mail per `NotifyApprovalWorkflowPort`.

## Dependencies

- **Depends on:** UC-M6-04 (Create Purchase Order).
- **Blocks:** UC-M6-06 (Receive Purchase Order), for orders above the alçada threshold.
