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

## How a controller obtains `CheckPermissionQuery.userId` (GRA-53)

A controller never accepts the caller's id from the client (a `@RequestParam userId` would let any
caller impersonate anyone, defeating the gate). Instead, the caller is resolved server-side from
the session token issued by UC-M10-05:

1. The client sends `Authorization: Bearer <sessionToken>` (the token returned by
   `POST /api/auth/login`) on every subsequent request.
2. A controller method parameter annotated `@br.gravita.adapters.inbound.controllers.security.AuthenticatedUser UserId callerId`
   is resolved by `AuthenticatedUserArgumentResolver` (a `HandlerMethodArgumentResolver`
   registered via `WebMvcConfiguration`), which reads the header and looks the token up through
   `SessionStorePort`.
3. A missing/malformed header or a token with no matching session raises
   `UnauthorizedException`, mapped by `ApiExceptionHandler` to `401 Unauthorized`.
4. The controller passes that `callerId` — not any client-supplied id — as
   `CheckPermissionQuery.userId` to `CheckPermissionUseCase.execute(...)` before invoking the
   underlying use case.

This is deliberately minimal (in-memory session store, no expiry/logout, no refresh) rather than a
full `spring-boot-starter-security` integration: the codebase has no other security infrastructure
yet, and this is enough to make every M1–M9 controller retrofit resolve the caller identically.
Swap `InMemorySessionStoreAdapter` for a persisted/expiring store (only the `SessionStorePort`
adapter changes) before running more than one app instance or requiring logout/expiry.

Example (illustrative; the 403-on-denial handling is left to whichever ticket wires
`CheckPermissionUseCase` into a given controller, e.g. GRA-52 for `AccessLogController`):

```java
@GetMapping
public ResponseEntity<AccessLogPageResponse> search(
        @AuthenticatedUser UserId callerId,
        @RequestParam(required = false) UUID userId, /* ...other filters... */) {
    if (!checkPermissionUseCase.execute(new CheckPermissionQuery(callerId, "system", "access-log", PermissionAction.VIEW))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    // ...
}
```
