# UC-M7-09 — Manage Opportunity (`ManageOpportunityUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.2 — Funil de vendas: a visual Kanban by stage (Prospecting, Proposal, Negotiation, Closed, Lost). Oportunidades: estimated value, probability, expected close date, owner.

## Description

CRUD for `Opportunity` plus stage transitions across the Kanban funnel. Covers creation, field updates and moving an opportunity to a new stage (`PROSPECTING → PROPOSAL → NEGOTIATION → CLOSED`/`LOST`). Every stage change appends a `StageTransition` record (`fromStage`, `toStage`, `timestamp`), which is the only history [`GetFunnelConversionUseCase`](uc-15-get-funnel-conversion.md) has to compute average cycle time.

## Port signature

```java
public interface ManageOpportunityUseCase {
    OpportunityView create(CreateOpportunityCommand command);
    OpportunityView update(UpdateOpportunityCommand command);
    OpportunityView changeStage(ChangeOpportunityStageCommand command);
}
```

`CreateOpportunityCommand`/`UpdateOpportunityCommand`: `customerId`, `estimatedValue`, `probability`, `expectedCloseDate`, `owner`. `ChangeOpportunityStageCommand`: `opportunityId`, `newStage`.

## Outbound ports required

- `OpportunityRepositoryPort`
- `StageTransitionRepositoryPort` (appended on every `changeStage` call)

## REST endpoint

`GET/POST/PATCH /api/crm/opportunities`

## Domain entities touched

- `Opportunity`
- `StageTransition` (created on `changeStage`)

## Acceptance criteria

- [ ] Opportunity created with all required fields (`customer`, `estimatedValue`, `probability`, `expectedCloseDate`, `owner`) and initial stage `PROSPECTING`.
- [ ] Stage transitions are restricted to the defined enum values; arbitrary jumps (e.g. `PROSPECTING → CLOSED`) are allowed per the doc's flat Kanban model unless the team decides otherwise.
- [ ] `CLOSED` and `LOST` are terminal — no further stage change once reached.
- [ ] Field updates (value, probability, close date, owner) are allowed at any non-terminal stage.
- [ ] Every `changeStage` call appends exactly one `StageTransition` record; the log is append-only, never edited or backfilled.

## Dependencies

- **Depends on:** None.
- **Blocks:** [UC-10 Log Interaction](uc-10-log-interaction.md), [UC-15 Get Funnel Conversion](uc-15-get-funnel-conversion.md).
