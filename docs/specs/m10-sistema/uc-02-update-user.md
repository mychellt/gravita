# UC-M10-02 — Update User (`UpdateUserUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§11.1 — User data, profile assignment and status must be editable after creation; 2FA remains mandatory whenever the effective profile is Administrator.

## Description

An administrator edits an existing user's name, e-mail, profile or active/inactive status. Precondition: the requester holds `system → users → edit` permission and the target `User` exists. Postcondition: the change is persisted and, per doc §11.2, recorded in the audit trail via DB trigger (no application-level call needed).

## Port signature

```java
public interface UpdateUserUseCase {
    void execute(UpdateUserCommand command);
}
```

`UpdateUserCommand`: `userId`, optional `name`, `email`, `profile: ProfileRef`, `status`. Only non-null fields are applied.

## Outbound ports required

- `UserRepositoryPort`
- `ProfileRepositoryPort`

## REST endpoint

`PATCH /api/users/{id}`

## Domain entities touched

- `User`
- `Profile`

## Acceptance criteria

- [ ] Switching `profile` to Administrator forces `twoFactorEnabled = true`.
- [ ] Setting `status` to inactive prevents future logins (checked by UC-M10-05).
- [ ] Unknown `userId` returns not-found.
- [ ] Duplicate e-mail on update is rejected.

## Dependencies

- **Depends on:** UC-M10-01 (`RegisterUserUseCase`) — the user must already exist.
- **Blocks:** None.
