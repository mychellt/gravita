# UC-M10-05 — Authenticate (`AuthenticateUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§11.1 — 2FA via TOTP (Google Authenticator, Authy) is mandatory for admin. §11.4 — HTTPS mandatory, bcrypt passwords.

## Description

A user logs in with e-mail/password; if the effective profile is Administrator (or `twoFactorEnabled` is set), a second step verifies a TOTP code before the session is granted. Precondition: an active `User` exists. Postcondition: a session/token is issued on success, and the attempt (successful or not) is recorded in `AccessLog` (UC-M10-07's write side).

## Port signature

```java
public interface AuthenticateUseCase {
    AuthResult execute(AuthenticateCommand command);
}
```

`AuthenticateCommand`: `email`, `rawPassword`, optional `totpCode` (required on the second call when `twoFactorEnabled`). `AuthResult`: `{status: AUTHENTICATED | TOTP_REQUIRED | REJECTED, sessionToken?}`.

## Outbound ports required

- `UserRepositoryPort`
- `TotpVerificationPort`
- `AccessLogRepositoryPort`

## REST endpoint

`POST /api/auth/login`, `POST /api/auth/2fa/verify`

## Domain entities touched

- `User`
- `AccessLog`

## Acceptance criteria

- [ ] Wrong password is rejected without revealing whether the e-mail exists.
- [ ] `twoFactorEnabled = true` blocks session issuance until a valid TOTP code is verified.
- [ ] Inactive users (per UC-M10-02) cannot authenticate.
- [ ] Every login attempt (success or failure) writes an `AccessLog` entry with IP and device.

## Dependencies

- **Depends on:** UC-M10-01 (user must exist).
- **Blocks:** Every other module's REST layer indirectly, since a session is a precondition for `CheckPermissionUseCase` (UC-M10-06) to have an authenticated user to check.
