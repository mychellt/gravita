# UC-M7-11 — Schedule Follow-up Task (`ScheduleFollowUpTaskUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.2 — Tarefas: follow-up scheduling with an app/e-mail alert; calendar integration.

## Description

Manually schedules a follow-up task for a salesperson, linked to an `Opportunity` or a `Customer`, with a due date and an alert channel. An alert fires via app or e-mail when the task is due.

## Port signature

```java
public interface ScheduleFollowUpTaskUseCase {
    FollowUpTaskView execute(ScheduleFollowUpTaskCommand command);
}
```

`ScheduleFollowUpTaskCommand`: `opportunityId` (optional), `customerId` (optional), `dueDate`, `owner`, `alertChannel: {APP, EMAIL}`.

## Outbound ports required

- `FollowUpTaskRepositoryPort`
- `SendFollowUpAlertPort`

## REST endpoint

`POST /api/crm/follow-ups`

## Domain entities touched

- `FollowUpTask`

## Acceptance criteria

- [ ] Task persisted with `dueDate`, `owner`, `alertChannel`, and a link to `Opportunity` and/or `Customer`.
- [ ] Alert is delivered via the selected channel at (or leading up to) the due date.
- [ ] Scheduling a task with a past `dueDate` is rejected.

## Dependencies

- **Depends on:** [UC-09 Manage Opportunity](uc-09-manage-opportunity.md) (when linked to an opportunity).
- **Blocks:** —
