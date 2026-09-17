# UC-M10-04 — Save Custom Profile (`SaveCustomProfileUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§11.1 — "Any combination of permissions can be saved as a reusable profile" (Perfil customizado), distinct from the six standard profiles.

## Description

An administrator builds an arbitrary combination of module/screen/action permissions and saves it as a new, named, reusable `Profile`, so it can be assigned to users the same way a standard profile is. Precondition: requester holds `system → profiles → create`. Postcondition: a new custom `Profile` exists and is selectable by UC-M10-01/UC-M10-02.

## Port signature

```java
public interface SaveCustomProfileUseCase {
    ProfileId execute(SaveCustomProfileCommand command);
}
```

`SaveCustomProfileCommand`: `name`, `permissions: [Permission]`. Returns the created `ProfileId`.

## Outbound ports required

- `ProfileRepositoryPort`

## REST endpoint

`PUT /api/profiles/{id}/permissions` (custom profiles use the same endpoint as standard ones — a new `id` with no pre-seeded name creates the custom profile; see the module spec's adapter table).

## Domain entities touched

- `Profile`
- `Permission`

## Acceptance criteria

- [ ] A custom profile's `name` must be unique among all profiles (standard and custom).
- [ ] The saved profile is retrievable and assignable exactly like a standard profile.
- [ ] Permission combination has no restriction beyond valid `module`/`screen`/`action` values.

## Dependencies

- **Depends on:** None.
- **Blocks:** UC-M10-01/UC-M10-02 (a custom profile becomes a valid `profile` reference for user assignment).
