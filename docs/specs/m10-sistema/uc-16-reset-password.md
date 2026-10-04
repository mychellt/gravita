# UC-M10-16 — Reset Password (`RequestPasswordResetUseCase` / `ResetPasswordUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14) — the login page (UC-M10-15) is not usable without a recovery path.
**Owner:** Product
**Status:** Draft for review

## Problem

The login page (UC-M10-15) must offer "Esqueci minha senha" (requirement 2 of GRA-207), but no password-recovery use case exists yet — `AuthenticateUseCase` can only check a password, not replace one. A user who forgets their password today has no self-service way back in.

## Goal

A user who forgot their password can request a reset by e-mail and be back on the login page with a working new password in one pass, without ever learning whether a given e-mail address has an account on the platform.

**Success measure:** a user who requests a reset and clicks the e-mailed link within the validity window can set a new password and log in, in one pass, without contacting support.

## Users

- **Any Gravita user** who knows their e-mail but not their current password.

## User story

> As a user who forgot my password, I want to ask for a reset link by e-mail, open it, and choose a new password, so that I can get back into Gravita without help from support.

## Scope

**In scope**
- "Esqueci minha senha" request step: user submits an e-mail address.
- The reset e-mail and its link.
- The reset page: set a new password, consuming the link's token.
- Token lifecycle: issuance, 1-hour expiry, single use, invalidated by a newer request.

**Out of scope** (separate tickets)
- **Password strength policy.** Registration today (`User.validate`) only requires a non-blank password — no length or complexity rule exists anywhere in the system. Adding one here only for reset would make the two entry points disagree, so this ticket keeps the same bar (non-blank) and leaves introducing a real policy as a cross-cutting follow-up that would touch registration too.
- **Changing password while logged in** (a "change password" settings screen with the current password as a check). Different job (a known-good session changing a credential) from this one (no session at all).
- **Notifying the user by e-mail after a successful reset** ("your password was changed, wasn't you? contact us"). Good practice, but not requested and not required to make the flow usable; flagged under "Risks."
- **Reasoning about why an address got no e-mail.** The platform never confirms or denies whether an address has an account (see "Resend/privacy" below) — there is deliberately no such path.

## Flow

1. From the login page, the user opens "Esqueci minha senha" and types their e-mail → `POST /api/auth/password-reset`.
2. The platform always answers the same way, whether or not the address has an account, and whether that account is active or still pending activation: *Se este e-mail tiver uma conta, enviaremos um link de redefinição em instantes.*
3. If the address belongs to an **active** user, a reset e-mail is sent with a link valid for **1 hour**, usable once. (A `PENDING_ACTIVATION` account cannot get a password reset — it has no password worth resetting yet; it gets the same neutral message and no e-mail, same privacy rule as activation resend.)
4. The user opens the link → the reset page, `GET /reset-password?token=<token>` (or the platform's chosen route) → user enters a new password and confirms it → `POST /api/auth/password-reset/confirm` with the token and the new password.
5. On success, the password is changed, every other unused reset token for that user is invalidated, and the user is sent to the login page to sign in with the new password (not logged in automatically — same choice as UC-M10-14 for activation, for the same reason: a fresh credential should be proven once at the login screen, not trusted blind).

## Outcomes and copy (reset page)

Mirrors the four-state pattern already approved for activation (UC-M10-14), adapted to this flow:

| # | State | Trigger | Title | Message | Primary action |
|---|---|---|---|---|---|
| 1 | Form | Valid, unused, unexpired token | Nova senha | Escolha uma nova senha para sua conta. | Form (new password + confirmation) → **Redefinir senha** |
| 2 | Success | Reset completed | Senha redefinida | Sua senha foi alterada. Você já pode entrar com a nova senha. | **Ir para o login** |
| 3 | Expired | Token older than 1 hour, never used | Link expirado | Este link de redefinição expirou. Peça um novo. | Back to the request step |
| 4 | Invalid | Unknown/missing/already-used token, or account no longer eligible (deactivated, or still pending activation) | Link inválido | Não foi possível redefinir sua senha com este link. Peça um novo. | Back to the request step |

Rules for the copy: never say "already used" distinctly from "invalid" here — unlike activation, where a used link still has a positive message ("your account is already active, go log in"), a *used reset link* has nothing good to confirm (revealing "this one was already used" could help an attacker probe which links were consumed), so states 3 and 4 do not need the extra nuance UC-M10-14 has. State 3 (expired) stays distinct because it tells the legitimate user the honest, actionable reason.

## Business rules

1. A reset link is valid for **1 hour** from issuance and usable **once**. *(Assumption, unconfirmed: chosen shorter than the 24-hour activation window because a reset token grants account takeover of an already-active account, not just e-mail verification of a brand-new one. Revisit if the user wants a different window.)*
2. Requesting a new reset e-mail invalidates every previous unused reset token for that user — only the newest link works (same rule as UC-M10-14).
3. The request step never reveals whether an address has an account, nor its status — one message for every case (unknown address, active account, pending-activation account, deactivated account).
4. Only an `ACTIVE` user's request actually sends an e-mail. `PENDING_ACTIVATION` and `INACTIVE` accounts are told the same neutral message but get no e-mail.
5. A second request for the same address within 60 seconds is ignored silently (same throttle as activation resend, UC-M10-14), to stop someone from mail-bombing an address.
6. New password validation is the same as registration today: required, non-blank. Confirmation field must match before submission is allowed (client-side check; not a new backend rule).
7. Completing a reset invalidates every other unused reset token for that user, and does **not** start a session — the user proves the new password once at login (consistent with UC-M10-14's "don't log in automatically after a credential event").

## Worked example

- `maria@padariadosol.com.br` (active) requests a reset at 14:00 → neutral message shown → e-mail sent with a link valid until 15:00, token `rst_8f21...`.
- She clicks the link at 14:10 → state 1, sets `novaSenha123` twice → `POST /api/auth/password-reset/confirm` → state 2 → clicks **Ir para o login** → logs in with `novaSenha123`.
- She requests a second reset by mistake at 14:12 → the 14:00 token is invalidated; only the 14:12 e-mail's link still works.
- A different user opens the original (now-invalidated) 14:00 link at 14:20 → state 4 (invalid), regardless of whether they know it was ever valid.
- Nobody opens the link at all; at 15:01 the same link → state 3 (expired).
- `naoexiste@padariadosol.com.br` (no account) requests a reset → identical neutral message as Maria got, no e-mail sent.

## Acceptance criteria

**Request step**
1. Given any e-mail address (existing active, existing pending, existing inactive, or unknown), when the user requests a reset, then the response/message is identical in every case.
2. Given an active user's address, when requested, then exactly one e-mail is sent with a link valid for 1 hour.
3. Given a pending-activation or inactive user's address, when requested, then no e-mail is sent, but the same neutral message is shown.
4. Given a second request for the same address inside 60 seconds, then no second e-mail is sent.

**Reset step**
5. Given a valid, unused, unexpired token, when the user submits a new password and it is accepted, then the password is changed and every other unused token for that user is invalidated.
6. Given a token older than 1 hour that was never used, then the page shows the expired state and the password is unchanged.
7. Given a token that was already used, an unknown token, or a token for an account that is `PENDING_ACTIVATION`/`INACTIVE`, then the page shows the invalid state and the password is unchanged.
8. Given a successful reset, then the user is sent to the login page and is **not** automatically signed in.
9. Given the new-password form, the confirmation field must match the new password before the user can submit.

**Quality**
10. No state shows a stack trace, HTTP status, or raw JSON; the token never appears in the browser's address bar after the page has read it, in logs, or in analytics.
11. The page is usable at 360px width and respects OS dark-mode preference, consistent with the rest of the public pages.

## Platform contract (new)

| Call | Result |
|---|---|
| `POST /api/auth/password-reset` `{email}` | `202` always, neutral body; `400` only for a malformed address |
| `POST /api/auth/password-reset/confirm` `{token, newPassword}` | `200` on success; `410`-equivalent outcome surfaced as the expired state; `400`/`404`-equivalent outcome surfaced as the invalid state — exact status codes are Atlas's call, mirroring how UC-M10-14 maps its outcomes |

Suggested use cases (naming to match the existing `AuthenticateUseCase`/`ActivateAccountUseCase` style): `RequestPasswordResetUseCase.execute(email)`, `ResetPasswordUseCase.execute(token, newPassword)`.

## Implementation note (non-binding — Atlas's call)

The activation flow already solved this exact shape of problem (issue a single-use, time-boxed token by e-mail; four-state result page; neutral resend message; invalidate-on-reissue) via `ActivationToken`, `ActivationTokenRepositoryPort`, and the `user_activation.ftl`/activation-tokens migration pattern. Reusing that shape for a `PasswordResetToken` (separate table/entity — a reset token must not accidentally satisfy activation, or vice versa) is the recommended path rather than inventing a new mechanism. Suggested config keys mirroring `gravita.activation.*`: `gravita.password-reset.link-base-url`, `gravita.password-reset.result-page-url`.

## Dependencies

- **Depends on:** UC-M10-01 (a user must exist to reset its password).
- **Blocks:** UC-M10-15 (Login Page) cannot show a *working* "Esqueci minha senha" link until this ships — the login page's entry point and this flow should land together or in quick succession, even though they are separate tickets.

## Risks / open questions

1. 1-hour expiry is an assumption, not a confirmed business rule — flagging as unconfirmed per the "legal/security parameters" guidance; cheap to change later (it's a single config value), so not blocking.
2. No "your password just changed, wasn't you?" notification e-mail in this scope — low cost to add later, listed under out-of-scope with reasoning above, not silently dropped.

## Changelog

- Draft created (this revision) — new use case, split out of GRA-207 because it is sized like UC-M10-14 (token issuance + e-mail + multi-state page), not like a one-line addition to the login page.
