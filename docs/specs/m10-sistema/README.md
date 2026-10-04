# M10 — Configurações e Sistema: implementation tickets

One ticket per use case from the [module spec](../m10-sistema.md), for granular phase planning. Most ship in Phase 1 (Fundação), since every other module depends on authentication, permissions and approval alçadas; audit-trail querying, SEFAZ monitoring, backup and alerting ship as hardening in Phase 8 (Robustez) — see [uc-08](uc-08-get-audit-trail.md#notes) for why the audit-trail *write* path (DB trigger) still starts in Phase 1.

| # | Ticket | Phase | Responsibility |
|---|--------|-------|-----------------|
| 01 | [Register User](uc-01-register-user.md) | 1 — Fundação | Create a user with credentials and an assigned profile. |
| 02 | [Update User](uc-02-update-user.md) | 1 — Fundação | Edit a user's data, profile or status. |
| 03 | [Assign Profile](uc-03-assign-profile.md) | 1 — Fundação | Set the permission set of a standard profile. |
| 04 | [Save Custom Profile](uc-04-save-custom-profile.md) | 1 — Fundação | Save an arbitrary permission combination as a reusable profile. |
| 05 | [Authenticate](uc-05-authenticate.md) | 1 — Fundação | Login + password + mandatory TOTP 2FA for admin. |
| 06 | [Check Permission](uc-06-check-permission.md) | 1 — Fundação | The authorization gate every other module's REST layer calls. |
| 07 | [Get Access Log](uc-07-get-access-log.md) | 1 — Fundação | Query login/logout/IP/device history (12-month retention). |
| 08 | [Get Audit Trail](uc-08-get-audit-trail.md) | 8 — Robustez | Admin-facing per-record change history query. |
| 09 | [Configure Approval Alçada](uc-09-configure-approval-alcada.md) | 1 — Fundação | Shared value-threshold approval configuration for `sales`/`purchasing`/`finance`. |
| 10 | [Configure Integration Credential](uc-10-configure-integration-credential.md) | 1 — Fundação | Register/rotate credentials for each external integration. |
| 11 | [Monitor SEFAZ Availability](uc-11-monitor-sefaz-availability.md) | 8 — Robustez | Availability polling for production/homologation SEFAZ endpoints. |
| 12 | [Trigger Backup](uc-12-trigger-backup.md) | 8 — Robustez | Daily DB + fiscal-XML backup, 90-day retention. |
| 13 | [Record Monitoring Alert](uc-13-record-monitoring-alert.md) | 8 — Robustez | Transmission failures, delayed jobs, critical errors. |
| 14 | [Activate Account](uc-14-activate-account.md) | 1 — Fundação | Confirm a new signup's e-mail through the emailed link; clear expired/used link handling. |
| 15 | [Login Page](uc-15-login-page.md) | 1 — Fundação | The page and route guard that front `AuthenticateUseCase`; TOTP step for admins. |
| 16 | [Reset Password](uc-16-reset-password.md) | 1 — Fundação | Self-service "forgot my password" flow the login page links to. |
