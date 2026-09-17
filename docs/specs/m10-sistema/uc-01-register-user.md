# UC-M10-01 — Register User (`RegisterUserUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§11.1 — Standard profiles (Administrator, Financial, Salesperson, Cashier Operator, Purchasing, Read-only) must be assignable to a user; 2FA via TOTP is mandatory for admin.

## Description

An administrator creates a new user account with credentials and an assigned profile. Precondition: the requester holds `system → users → create` permission. On success, a `User` is persisted with a bcrypt-hashed password and `twoFactorEnabled` forced to `true` when the assigned profile is Administrator; the user can then authenticate (UC-M10-05).

## Port signature

```java
public interface RegisterUserUseCase {
    UserId execute(RegisterUserCommand command);
}
```

`RegisterUserCommand`: `name`, `email`, `rawPassword`, `profile: ProfileRef` (`User`/`Profile` per the module's domain model). Returns the created `UserId`.

## Outbound ports required

- `UserRepositoryPort`
- `ProfileRepositoryPort`

## REST endpoint

`POST /api/users`

## Domain entities touched

- `User`
- `Profile`

## Acceptance criteria

- [ ] Password is stored as a bcrypt hash, never plaintext (doc §11.4).
- [ ] Assigning the Administrator profile forces `twoFactorEnabled = true`.
- [ ] Duplicate e-mail is rejected.
- [ ] Unknown `profile` reference is rejected.

## Dependencies

- **Depends on:** UC-M10-03 (`AssignProfileUseCase`) or a pre-seeded standard profile must exist to reference.
- **Blocks:** UC-M10-05 (`AuthenticateUseCase`) — a user must exist before it can log in.
