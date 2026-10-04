# UC-M10-15 — Login Page

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system` (consumes the existing `AuthenticateUseCase`, UC-M10-05)
**Roadmap phase:** Phase 1 — Fundação (doc §14)
**Owner:** Product
**Status:** Draft for review

## Problem

`AuthenticateUseCase` and its REST endpoints (`POST /api/auth/login`, `POST /api/auth/2fa/verify`) already exist (UC-M10-05), but there is no page a user can open to call them. Every other screen in the app (`/dashboard`, `/pdv`, `/nfe`, …) is reachable today with no session at all — there is no route guard. The product has no front door and no lock on the back doors.

## Goal

A user who knows their e-mail and password can get from "not logged in" to the dashboard in one screen, the same way they would expect from any Gravita page. A user who forgot their password has a visible way out that does not involve contacting support.

**Success measure:** 100% of the existing authenticated routes (`/dashboard`, `/pdv`, `/nfe`, `/nfse`, `/inventory`, `/purchasing`, `/crm`, `/finance`, `/reports`, `/settings`, and their `/admin/*` counterparts) require a valid session; today none of them do.

## Users

- **Any Gravita user** (Administrator, Financial, Salesperson, Cashier Operator, Purchasing, Read-only) — logs in once per session/device.
- **Administrator** — additionally completes a TOTP step (`twoFactorEnabled` is mandatory for this profile, per UC-M10-05).

## User story

> As a Gravita user, I want to enter my e-mail and password on a page that looks like the rest of the system and land straight on my dashboard, so that I can start working without extra steps. If I forgot my password, I want a clearly visible way to recover it from the same screen.

## Scope

**In scope**
- The login page itself: e-mail + password form, submit, error state.
- The second step for accounts with `twoFactorEnabled = true` (TOTP code entry), since those accounts cannot otherwise complete login.
- A visible "Esqueci minha senha" entry point that starts the flow specified in [UC-M10-16 — Reset Password](uc-16-reset-password.md).
- Redirecting to the dashboard (`/dashboard`) on success.
- A route guard so the authenticated routes listed above refuse an anonymous visitor and send them to the login page instead.

**Out of scope** (separate tickets, not needed for this page to be usable)
- **Logout / session termination UI.** Nothing in the current ticket asked for it, and `AuthenticateUseCase` has no counterpart logout use case yet. Needed soon, but it is a distinct job ("leave safely") from this one ("get in").
- **Deep-link return** (sending the user back to the page they originally requested instead of always to `/dashboard`). Requirement 5 is explicit — always the dashboard — so preserving the original destination is a nice-to-have for later, not a rule here.
- **"Remember me" / persistent sessions, password strength policy, login rate-limiting or account lockout after repeated failures.** All real gaps (the backend currently never locks an account after repeated bad attempts), but they are backend/security hardening independent of whether a page exists, and match the module README's existing split between Phase 1 (get the door working) and Phase 8 — Robustez (harden the lock). Flagged below under "Risks," not silently dropped.
- **Platform-admin login** (`/admin/*` shell). It is covered by the same route guard and the same `AuthenticateUseCase`/session, so no extra product rule is needed, but its own branding/copy is unchanged by this ticket.

## Business rules

1. The login identifier is the user's e-mail (requirement 3). E-mail uniqueness is already enforced at account creation (`SignupService`/`RegisterUserUseCase`, UC-M10-01) — this page does not add a new uniqueness rule, it relies on the existing one (requirement 4).
2. A rejected login (wrong password, unknown e-mail, inactive account, or an account still pending e-mail activation) shows exactly one generic message and never reveals which of those reasons applies. This is not a new rule — `AuthenticateService.reject(...)` already treats all of them identically — the page must not add detail the backend deliberately withholds.
3. An account with `twoFactorEnabled = true` (every Administrator, per UC-M10-05) cannot complete login with e-mail/password alone. The page must offer the TOTP step; there is no way to skip it for those accounts.
4. An invalid TOTP code is a distinct error from step 1's generic rejection — by the time the user reaches this step they have already proven they know the correct password, so naming the problem ("invalid code") does not leak account existence.
5. On `AUTHENTICATED`, the page stores the returned `sessionToken` and sends the user to `/dashboard`. On any other result, the user stays on the login page.
6. Any of the authenticated routes, opened without a valid session, redirects to the login page instead of rendering.

## Flow

1. Anonymous visitor opens the app at any authenticated route (including `/` and `/dashboard` directly) → route guard sends them to the login page.
2. User enters e-mail + password, submits → `POST /api/auth/login`.
3. **If `AUTHENTICATED`:** session token stored, user is sent to `/dashboard`.
4. **If `TOTP_REQUIRED`:** the page swaps to a one-field TOTP step (the e-mail/password are not re-entered); user enters the 6-digit code from their authenticator app → `POST /api/auth/2fa/verify` with the same e-mail and the code. Loops back to step 3 or shows the invalid-code error and lets the user retry.
5. **If `REJECTED`:** generic error shown, form stays filled in (except password), focus returns to the e-mail field.

## Worked example

- `maria@padariadosol.com.br`, profile Cashier Operator, `twoFactorEnabled = false`. Submits correct password → `200 {status: AUTHENTICATED, sessionToken: "7f1e...od2"}` → redirected to `/dashboard`.
- `joao@padariadosol.com.br`, profile Administrator, `twoFactorEnabled = true`. Submits correct password → `200 {status: TOTP_REQUIRED, sessionToken: null}` → page shows the code field → submits `482913` → `POST /api/auth/2fa/verify` → `200 {status: AUTHENTICATED, sessionToken: "9ab3...e40"}` → redirected to `/dashboard`.
- `maria@padariadosol.com.br` submits the wrong password → `401 {status: REJECTED}` → page shows "E-mail ou senha incorretos." — identical to what an unknown e-mail or a not-yet-activated account would show.
- `joao@padariadosol.com.br` reaches the TOTP step and types `000000` → `401 {status: REJECTED}` → page shows "Código de verificação inválido." and lets him try again.

## UX requirements (doc §12.1, applies here as everywhere)

- Same visual language as the rest of the public pages (`register.html`, `activate.html`): DM Sans/DM Mono type, Tabler icons, pt-BR copy, same spacing and component look.
- Error text states what to do ("Confira seu e-mail e senha e tente novamente."), never a raw HTTP status or stack trace.
- Dark mode respected (OS preference), per the global rule.
- Usable down to 360px width; the submit button and both form steps are reachable without horizontal scrolling.
- The page never auto-fills or echoes the password in any error state.

## Acceptance criteria

**Login**
1. Given a user with a correct e-mail and password and `twoFactorEnabled = false`, when they submit, then they land on `/dashboard` with a stored session.
2. Given a user with a correct e-mail and password and `twoFactorEnabled = true`, when they submit, then they see the TOTP step and are **not** redirected until a valid code is also submitted.
3. Given a wrong password, an unknown e-mail, an `INACTIVE` account, or a `PENDING_ACTIVATION` account, when the user submits e-mail/password, then they see the same single generic error message in all four cases, and remain on the login page.
4. Given the TOTP step, when an invalid code is submitted, then the user sees a code-specific error and can retry without re-entering e-mail/password.
5. Given a successful login, then the dashboard is the only redirect target (never the page the user originally tried to open).

**Route guard**
6. Given no active session, when any of `/dashboard`, `/pdv`, `/nfe`, `/nfse`, `/inventory`, `/purchasing`, `/crm`, `/finance`, `/reports`, `/settings`, or any `/admin/*` route is opened directly, then the user is sent to the login page and the protected view never renders.
7. Given an active session, when the login page is opened directly, then the user is sent straight to `/dashboard` instead of seeing the form again.

**Forgot password entry point**
8. Given the login page, then a visibly-labeled "Esqueci minha senha" action is present and starts the flow in UC-M10-16.

**Quality**
9. No error state shows an HTTP status code, stack trace, or raw JSON.
10. The page is usable at 360px width and respects the OS dark-mode preference.

## Platform contract (existing, unchanged by this ticket)

| Call | Result |
|---|---|
| `POST /api/auth/login` `{email, password}` | `200 {status: AUTHENTICATED, sessionToken}` / `200 {status: TOTP_REQUIRED}` / `401 {status: REJECTED}` |
| `POST /api/auth/2fa/verify` `{email, password, totpCode}` | same result shapes as above |

No backend change is required for this ticket — `AuthenticateUseCase`, `AuthController`, `SessionStorePort` already implement this contract (UC-M10-05). This ticket is page + route-guard work.

## Implementation note (non-binding — Atlas's call)

Today the unauthenticated flows (`register.html`, `activate.html`) are static pages under `web/public/`, while every authenticated screen (`/dashboard`, `/pdv`, …) is an Angular route with no guard yet. Recommendation: build the login page as an Angular route (e.g. `/login`, outside `ShellComponent`) rather than a third static page, so the session it creates and the `CanActivate` guard it needs live in the same app as the routes they protect, using one `AuthService`/HTTP interceptor as the single source of truth for "is there a session" — avoiding a full-page handoff between two different front-ends for something as central as auth state. This is an architecture choice, not a product requirement; the acceptance criteria above hold regardless of which surface it's built on.

## Dependencies

- **Depends on:** UC-M10-05 (`AuthenticateUseCase` + REST endpoints — already implemented), UC-M10-01 (e-mail uniqueness at registration).
- **Related:** UC-M10-16 (Reset Password) — this page only needs the entry point to exist; the recovery flow itself ships on its own ticket.
- **Blocks:** nothing functionally (the backend already works), but this is the first ticket that makes `/dashboard` and the other module routes actually require a session — downstream module work should not assume an always-open app after this ships.

## Risks / open questions

1. **Login attempt throttling / account lockout does not exist today** (`AuthenticateService` never counts failures). Not blocking this ticket, but should be scheduled before go-live — flagging for a Phase-8-style hardening ticket rather than silently leaving it.
2. Should the TOTP step show a "resend/having trouble?" path for an admin who lost their authenticator device? Not specified anywhere yet (UC-M10-05 doesn't cover TOTP recovery). Out of scope here; needs its own decision if it becomes a real support burden.

## Changelog

- Draft created (this revision) — covers the page, the TOTP step, the route guard, and the forgot-password entry point; backend already exists via UC-M10-05.
