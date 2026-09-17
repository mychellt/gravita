# UC-M7-10 — Log Interaction (`LogInteractionUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.2 — Interações: log of calls, visits, e-mails, WhatsApp — with date, time, summarized content.

## Description

Records a CRM interaction — a call, visit, e-mail or WhatsApp exchange — against an opportunity or directly against a customer, with a timestamp and a short summary. Interaction history feeds the customer timeline (M1 §2.2) and the automatic follow-up rule evaluation ([UC-12](uc-12-evaluate-follow-up-rules.md)).

## Port signature

```java
public interface LogInteractionUseCase {
    InteractionView execute(LogInteractionCommand command);
}
```

`LogInteractionCommand`: `opportunityId` (optional), `customerId` (optional, at least one of the two required), `channel: {CALL, VISIT, EMAIL, WHATSAPP}`, `summary`, `timestamp`.

## Outbound ports required

- `InteractionRepositoryPort`

## REST endpoint

`POST /api/crm/opportunities/{id}/interactions`

## Domain entities touched

- `Interaction`

## Acceptance criteria

- [ ] Interaction stored with `channel`, `summary` and `timestamp`.
- [ ] Interaction links to an `Opportunity`, a `Customer`, or both.
- [ ] At least one of `opportunityId`/`customerId` is required; omitting both is rejected.

## Dependencies

- **Depends on:** [UC-09 Manage Opportunity](uc-09-manage-opportunity.md) (when linked to an opportunity).
- **Blocks:** [UC-12 Evaluate Follow-up Rules](uc-12-evaluate-follow-up-rules.md).
