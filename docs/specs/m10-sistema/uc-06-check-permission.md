# UC-M10-06 — Check Permission (`CheckPermissionUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§11.1 — "Permissões granulares: por módulo → tela → ação (visualizar, criar, editar, excluir, aprovar, exportar)."

## Description

Given an authenticated user and a requested `(module, screen, action)`, resolves whether the user's profile grants it. Every other context's `adapter.in.web` layer calls this before executing a use case — it is the authorization gate referenced throughout every other module spec's "Cross-module dependencies" section. Precondition: a valid session (UC-M10-05). Postcondition: allow/deny, no state change.

## Port signature

```java
public interface CheckPermissionUseCase {
    boolean execute(CheckPermissionQuery query);
}
```

`CheckPermissionQuery`: `userId`, `module`, `screen`, `action`. Returns `true` if the user's effective `Profile.permissions` contains a matching `Permission`.

## Outbound ports required

- `UserRepositoryPort`
- `ProfileRepositoryPort`

## REST endpoint

None directly — invoked in-process by every other context's controllers/interceptors, not exposed as its own HTTP endpoint.

## Domain entities touched

- `User` (read-only)
- `Profile` (read-only)
- `Permission` (read-only)

## Acceptance criteria

- [ ] A user whose profile lacks the exact `(module, screen, action)` triple is denied.
- [ ] Denials never leak which permission would have been required beyond the standard 403 response.
- [ ] Works identically for standard and custom profiles.
- [ ] Result reflects the latest `AssignProfileUseCase`/`SaveCustomProfileUseCase` write — no stale cache across requests.

## Dependencies

- **Depends on:** UC-M10-03 (`AssignProfileUseCase`), UC-M10-04 (`SaveCustomProfileUseCase`), UC-M10-05 (`AuthenticateUseCase`).
- **Blocks:** Essentially every other module's REST layer — no controller in M1–M9 should ship without calling this first.
