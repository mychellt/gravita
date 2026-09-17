# UC-M4-06 — Manage Discrimination Template (`ManageDiscriminationTemplateUseCase`)

**Module:** M4 — Fiscal: NFSe ([module spec](../m4-fiscal-nfse.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 6 — Serviços (doc §14)

## Functional requirement

Discrimination: a rich free-text field, with a configurable template per service type to speed up issuance (§5.2).

## Description

CRUD for `DiscriminationTemplate`s, keyed by service type. A user manages the set of reusable templates that `IssueRpsUseCase` can pre-fill into the RPS's free-text discrimination field, so recurring service descriptions don't need to be retyped each time.

## Port signature

```java
public interface ManageDiscriminationTemplateUseCase {
    DiscriminationTemplateId create(CreateDiscriminationTemplateCommand command);
    void update(UpdateDiscriminationTemplateCommand command);
    void delete(DiscriminationTemplateId id);
    List<DiscriminationTemplate> list(ServiceCode serviceType);
}
```

`CreateDiscriminationTemplateCommand`/`UpdateDiscriminationTemplateCommand`: `serviceCode`, `templateText`.

## Outbound ports required

- `NfseRepositoryPort` (or a dedicated template repository if the module spec's persistence adapters split it out — the module spec keeps templates within the same persistence surface as `NfseDocument`)

## REST endpoint

`GET/POST /api/nfse/discrimination-templates`

## Domain entities touched

- `DiscriminationTemplate`

## Acceptance criteria

- [ ] Templates can be created, updated, listed and deleted per service type.
- [ ] Listing by service type returns only templates applicable to that type.
- [ ] Deleting a template does not affect discrimination text already saved on previously issued RPS/NFSe.

## Dependencies

- **Depends on:** None.
- **Blocks:** —
