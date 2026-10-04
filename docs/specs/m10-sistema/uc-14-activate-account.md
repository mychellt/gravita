# UC-M10-14 — Activate Account (`ActivateAccountUseCase`)

**Module:** M10 — Configurações e Sistema ([module spec](../m10-sistema.md))
**Context package:** `br.gravita.system`
**Roadmap phase:** Phase 1 — Fundação (doc §14)
**Owner:** Product
**Status:** Draft for review

## Problem

A person who signs up on the register page (`register.html`) gets an account that cannot log in until the e-mail address is confirmed. Today the e-mailed link lands on a raw API URL that answers with JSON, so the person sees a technical response instead of knowing whether their account is ready. When the link is old or already used, they get no way forward and are likely to give up or contact support.

## Goal

When a new user clicks the link in the activation e-mail, they land on a Gravita page that clearly says **whether their account is now active** and what to do next. If the link no longer works, the page says why and lets them get a new one in one step.

**Success measures**
- At least 90% of signups that click the link end on the "Conta ativada" state.
- Fewer than 5% of failed-link visits end without a resend request or a login.
- Support tickets tagged "não recebi / não consigo ativar" fall after release.

## Users

- **New customer (primary):** the person who just registered a company and is the first administrator. Non-technical, usually opens the e-mail on the same day and may be on a phone.
- **Returning user (secondary):** someone who clicks an old e-mail again, or clicks twice.

## User story

> As a person who just signed up, I want to click the link in my e-mail and immediately see that my account is activated, so that I can log in and start using Gravita. If the link has expired, I want to be told so and get a new one without starting over.

## Scope

**In scope**
- The activation page that the e-mail link opens, and its four states (loading, activated, link expired, link invalid or already used).
- Requesting a new activation e-mail from that page.
- The link target in the e-mail (it must open the page, not the API).

**Out of scope** (separate tickets)
- Changing the e-mail's content or design.
- Logging the user in automatically after activation. They go to the login screen.
- Changing the e-mail address of a pending account.
- Password reset.

## Flow

1. The user receives the activation e-mail, valid for **24 hours** from the moment it was issued.
2. The user clicks **Ativar minha conta**. The browser opens `activate.html?token=<token>`.
3. The page shows a loading state, then asks the platform to activate the account.
4. The page shows exactly one of the outcomes below.

## Outcomes and copy

All user-facing text is in Brazilian Portuguese. Wording below is the approved copy.

| # | Situation | Title | Message | Primary action |
|---|---|---|---|---|
| 1 | Loading | Ativando sua conta… | — | none |
| 2 | **Account activated** | Conta ativada | Tudo certo! Seu e-mail foi confirmado e você já pode entrar. | **Ir para o login** |
| 3 | **Link expired** (older than 24 h, never used) | Link expirado | Este link de ativação expirou. Peça um novo abaixo. | Reenviar e-mail (form with e-mail field) |
| 4 | **Link already used** (account was already activated with it) | Link já utilizado | Este link de ativação já foi utilizado. Se a sua conta já está ativa, é só entrar. | **Ir para o login**; resend form as secondary |
| 5 | **Link invalid** (unknown token, damaged link, token missing) | Link inválido | Não foi possível ativar sua conta com este link. Peça um novo abaixo. | Reenviar e-mail |
| 6 | Platform unreachable | Não foi possível ativar agora | Tente novamente em instantes. | **Tentar novamente** (reload) |

Rules for the copy:
- State 2 must be the only state that says the account is active.
- State 3 must say the link expired, never only "inválido", so the user knows it was not their mistake.
- The page must never reveal whether a given e-mail address has an account (see "Resend").

## Business rules

1. An activation link is valid for 24 hours and can be used **once**.
2. Activating moves the user from "aguardando ativação" to active. The user can log in only after that.
3. Clicking the same link again after success is not an error for the user: they see state 4 with a login button, and the account stays active.
4. An expired link never activates the account, and does not change its state.
5. Only the newest link works: requesting a new e-mail invalidates the previous links of that user.
6. A deactivated (blocked) account is never reopened by an old link. It sees state 5.

## Resend (states 3, 4 and 5)

- The user types their e-mail and presses **Reenviar**. The page always answers with the same message, whether or not an account exists for that address: *Se este e-mail estiver aguardando ativação, enviaremos um novo link em instantes.* This protects customers' privacy.
- A second request for the same address within **60 seconds** is ignored silently, and the button stays disabled for that period with the label "Enviado!".
- An empty or malformed address shows *Informe um e-mail válido.* next to the field and sends nothing.

## Acceptance criteria

**Activation**
- [ ] Given a valid, unused link, when the user opens it, then the page shows state 2 and the account becomes active.
- [ ] Given the account is active, when the user clicks **Ir para o login**, then they reach the login screen.
- [ ] Given the page is open and the request is in flight, then state 1 is shown and no other state is visible.

**Expired link**
- [ ] Given a link issued more than 24 hours ago, when the user opens it, then the page shows state 3 and the account stays inactive.
- [ ] Given state 3, then a resend form is visible and focused on the e-mail field.
- [ ] Given the user submits a valid e-mail, then they see the neutral confirmation message and a new e-mail is sent if the account is pending.

**Used, invalid and error links**
- [ ] Given a link that was already used, then the page shows state 4 and the account is unchanged.
- [ ] Given a link with an unknown or missing token, then the page shows state 5.
- [ ] Given the platform does not respond, then the page shows state 6 and **Tentar novamente** repeats the request.
- [ ] No state shows a stack trace, HTTP code, or raw JSON.

**Link in the e-mail**
- [ ] The **Ativar minha conta** button and the plain-text link both open the activation page (`activate.html?token=…`), not an API address.

**Resend**
- [ ] The response is identical for an unknown address, an already-active account and a pending account.
- [ ] A second request inside 60 seconds sends no second e-mail.
- [ ] After a resend, the previous link shows state 3 or 4, and only the new link activates.

**Quality**
- [ ] Works on mobile widths from 360 px, with the action buttons reachable without scrolling.
- [ ] Keyboard and screen-reader friendly: the state title is announced when it appears, buttons have visible focus, errors are not conveyed by color alone.
- [ ] The token never appears in logs, analytics or the browser history after the page loads.

## Platform contract (existing)

| Call | Result |
|---|---|
| `GET /api/activate?token=…` | `200` activated; `410` link expired (`reason: EXPIRED`) or already used (`reason: ALREADY_USED`); `400` unknown or missing token (`reason: INVALID`) |
| `POST /api/activate/resend` with `{ "email": … }` | `202` always, with the neutral message; `400` for a malformed address |

Use case: `ActivateAccountUseCase.execute(rawToken)`; resend: `ResendActivationUseCase.execute(email)`.

## Gaps against the current build

These are known differences between this spec and what exists today, to be planned as part of this ticket:

1. **E-mail link target.** `gravita.activation.link-base-url` defaults to `…/api/activate`, so the link opens JSON. It must point at the page (for example `https://<host>/activate.html`). Local default needs the same change.
2. **State 4 (already used).** `activate.html` currently shows "Link inválido ou expirado" for an already-used link. It needs its own title, copy and login action.
3. **Wording of state 5** should read "Link inválido", not "inválido ou expirado".
4. **State 6** (platform unreachable) has no retry action today.

## Analytics

Track, without the token or the e-mail address in any payload: `activation_page_viewed`, `activation_succeeded`, `activation_link_expired`, `activation_link_used`, `activation_link_invalid`, `activation_resend_requested`, `activation_login_clicked`.

## Dependencies

- **Depends on:** UC-M10-01 (a user created by signup starts as pending) and the signup flow that issues the token and sends the e-mail.
- **Blocks:** UC-M10-05 (`AuthenticateUseCase`): a pending user cannot log in.

## Open questions

1. Should the link stay valid for 24 hours, or is 48 hours better for people who register late in the day? (Current: 24 h.)
2. After activation, should we go to the login screen (this spec) or sign the user in directly?
3. Should state 4 offer the resend form at all, or only the login button?