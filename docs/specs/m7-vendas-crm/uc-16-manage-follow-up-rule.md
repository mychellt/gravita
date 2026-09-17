# UC-M7-16 — Manage Follow-up Rule (`ManageFollowUpRuleUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.2 — Follow-up automático: rules like "if no contact in X days, notify the salesperson" — no coding required.

## Description

CRUD for `FollowUpRule` definitions — the data-driven rules [`EvaluateFollowUpRulesUseCase`](uc-12-evaluate-follow-up-rules.md) reads on its scheduled run. Added after the initial split: the module spec's domain model already described `FollowUpRule` as "declarative... not hand-coded per case," but no use case managed its lifecycle. Without this, "no coding required" would be false — a new rule would need a code change to insert.

## Port signature

```java
public interface ManageFollowUpRuleUseCase {
    FollowUpRuleView create(CreateFollowUpRuleCommand command);
    FollowUpRuleView update(UpdateFollowUpRuleCommand command);
    void delete(FollowUpRuleId ruleId);
}
```

`CreateFollowUpRuleCommand`/`UpdateFollowUpRuleCommand`: `daysWithoutContact` (threshold), `target: {CUSTOMER, OPPORTUNITY}` (scope the rule applies to), `notifyOwner: boolean`, `active: boolean`.

## Outbound ports required

- `FollowUpRuleRepositoryPort`

## REST endpoint

`GET/POST/PATCH /api/crm/follow-up-rules`

## Domain entities touched

- `FollowUpRule`

## Acceptance criteria

- [ ] A new rule is active immediately for the next scheduled run of `EvaluateFollowUpRulesUseCase` — no deployment involved.
- [ ] `daysWithoutContact` must be a positive integer; rejected otherwise.
- [ ] Deleting a rule stops it from being evaluated on the next run; it doesn't retroactively affect notifications already sent.
- [ ] An inactive (`active: false`) rule is stored but skipped by evaluation.

## Dependencies

- **Depends on:** None.
- **Blocks:** [UC-12 Evaluate Follow-up Rules](uc-12-evaluate-follow-up-rules.md).
