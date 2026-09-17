# UC-M7-15 — Get Funnel Conversion (`GetFunnelConversionUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.2 — Conversão do funil: conversion rate per stage, average cycle time, volume per salesperson.

## Description

Analytics query over the CRM funnel: conversion rate between consecutive stages, average time an opportunity spends moving from `PROSPECTING` to `CLOSED`, and opportunity volume broken down per salesperson, for a given period. Cycle time is computed from the `StageTransition` log (see [UC-09](uc-09-manage-opportunity.md)), not from `Opportunity.stage` alone.

## Port signature

```java
public interface GetFunnelConversionUseCase {
    FunnelConversionView execute(GetFunnelConversionQuery query);
}
```

`GetFunnelConversionQuery`: `period`, `salesperson` (optional). Returns `FunnelConversionView{conversionRateByStage, averageCycleTime, volumeBySalesperson}`.

## Outbound ports required

- `OpportunityRepositoryPort`
- `StageTransitionRepositoryPort` (source of cycle-time data)

## REST endpoint

`GET /api/crm/funnel/conversion`

## Domain entities touched

- `Opportunity` (read-only)
- `StageTransition` (read-only)

## Acceptance criteria

- [ ] Conversion rate is reported per stage transition (e.g. Prospecting → Proposal, Proposal → Negotiation, …).
- [ ] Average cycle time covers opportunities that reached `CLOSED` within the period.
- [ ] Volume is broken down per salesperson when `salesperson` is omitted, or filtered to one when provided.

## Dependencies

- **Depends on:** [UC-09 Manage Opportunity](uc-09-manage-opportunity.md) (source of `StageTransition` records).
- **Blocks:** —
