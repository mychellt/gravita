# UC-M7-12 — Evaluate Follow-up Rules (`EvaluateFollowUpRulesUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.2 — Follow-up automático: rules like "if no contact in X days, notify the salesperson" — no coding required.

## Description

A scheduled job that evaluates every configured `FollowUpRule` against each customer/opportunity's interaction history and notifies the owning salesperson when a rule's threshold is breached (e.g. no `Interaction` logged within the rule's configured day count). Rule thresholds are data, not code — new rules require no deployment.

## Port signature

```java
public interface EvaluateFollowUpRulesUseCase {
    void execute();
}
```

No command payload — triggered on a schedule (background job), not by a user action.

## Outbound ports required

- `FollowUpRuleRepositoryPort` (reads active rule definitions)
- `InteractionRepositoryPort`
- `SendFollowUpAlertPort`

## REST endpoint

None — background job, not exposed via REST.

## Domain entities touched

- `FollowUpRule`
- `Interaction` (read-only)

## Acceptance criteria

- [ ] A rule with a configured day threshold triggers a notification when the most recent `Interaction` for its target (customer/opportunity) is older than the threshold.
- [ ] Adding or editing a `FollowUpRule` changes evaluated behavior without a code change.
- [ ] A target with no interactions at all is treated as breaching every applicable rule.
- [ ] Running the job twice in the same period doesn't send duplicate notifications for the same breach.

## Dependencies

- **Depends on:** [UC-10 Log Interaction](uc-10-log-interaction.md) for interaction history; [UC-16 Manage Follow-up Rule](uc-16-manage-follow-up-rule.md) for rule definitions.
- **Blocks:** —
