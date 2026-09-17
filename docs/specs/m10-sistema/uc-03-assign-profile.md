# UC-M10-03 — Assign Profile (`AssignProfileUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§11.1 — Granular permissions per module → screen → action (view, create, edit, delete, approve, export), grouped into standard profiles: Administrator, Financial, Salesperson, Cashier Operator, Purchasing, Read-only.

## Description

Defines or updates the permission set of a standard `Profile` (one of the six seeded profiles). Precondition: requester holds `system → profiles → edit`. Postcondition: the `Profile`'s `permissions` list is replaced with the given set; every `User` referencing that profile picks up the change on next `CheckPermissionUseCase` call (no caching of permissions at the user level).

## Port signature

```java
public interface AssignProfileUseCase {
    void execute(AssignProfileCommand command);
}
```

`AssignProfileCommand`: `profileId`, `permissions: [Permission]` (each `Permission` is `module`, `screen`, `action`).

## Outbound ports required

- `ProfileRepositoryPort`

## REST endpoint

`PUT /api/profiles/{id}/permissions`

## Domain entities touched

- `Profile`
- `Permission`

## Acceptance criteria

- [ ] Replacing permissions on a standard profile does not affect other profiles.
- [ ] Every `Permission.action` is one of `VIEW, CREATE, EDIT, DELETE, APPROVE, EXPORT`.
- [ ] Unknown `profileId` returns not-found.

## Dependencies

- **Depends on:** None (standard profiles are seeded data).
- **Blocks:** UC-M10-01/UC-M10-02 (user registration/update reference a `Profile`), UC-M10-06 (`CheckPermissionUseCase` reads the profile's permission set).
