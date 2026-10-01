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

## Implementation notes

- **Persistence.** A dedicated `DiscriminationTemplateRepositoryPort` / table `discrimination_templates` (migration `V75`) rather than `NfseRepositoryPort`, since templates have no relation to the `NfseDocument` lifecycle. Delete is a hard delete.
- **Endpoints.** The spec lists `GET/POST /api/nfse/discrimination-templates`; update and delete need an addressable resource, so `PUT /{id}` and `DELETE /{id}` were added. `POST` → `201 {"id"}`; `GET [?serviceCode=]` → `200` list of `{id, serviceCode, templateText}` (all templates when the filter is omitted); `PUT`/`DELETE` → `204`. Errors: `404` unknown id on update/delete; `409` invalid service code (not LC 116 format/list) or blank text; `400` missing body fields.
- **Service type.** `serviceCode` is normalised through `ServiceCode.of` (`1.05`, `0105` and `01.05` are the same type), so filtering matches regardless of input form. Several templates may exist for the same service type; they list in creation order. Only the LC 116 structural check applies — no municipal-list check, since a template is not tied to a municipality.
- **AC3.** A document copies the text into `nfse_documents.discrimination` and keeps no reference to a template, so edits/deletes never reach issued RPS/NFSe (covered by an end-to-end test).
- **Not done here.** The pre-fill in `IssueRpsUseCase` is unchanged (`discrimination` is still plain text there); that integration is optional per the ticket and left to M4-02 follow-up. Update/delete take no ownership/authorization check — templates are global, not per company.
