# M10 — Configurações e Sistema

Source: ERP MVP doc §11. Context package: `br.gravita.system`.

## Purpose

Cross-cutting platform concerns that every other module depends on: users, permissions, audit, integrations, and infrastructure/security baseline.

## Functional requirements

### 11.1 Usuários e Permissões

- Perfis padrão: Administrator, Financial, Salesperson, Cashier Operator, Purchasing, Read-only.
- Permissões granulares: per module → screen → action (view, create, edit, delete, approve, export).
- 2FA: two-factor authentication via TOTP (Google Authenticator, Authy); mandatory for admin.
- Log de acesso: login, logout, IP, device — kept for 12 months.
- Perfil customizado: any permission combination can be saved as a reusable profile.

### 11.2 Auditoria

- Trilha completa: every action logged — user, date, time, screen, changed field, old value, new value.
- Imutabilidade fiscal: a cancelled NF keeps its record; physical deletion of fiscal documents is impossible.
- Consulta histórico: any system record has a "change history" tab, accessible to admin.

### 11.3 Integrações

- SEFAZ: NFe, NFCe, NFSe — production and homologation; availability monitoring.
- Receita Federal: CNPJ lookup for auto-filling registrations.
- ViaCEP / IBGE: address fill by CEP; updated IBGE municipality table.
- Bancos: boleto, PIX cobrança, CNAB 240/400 remittance, OFX return.
- WhatsApp Business API: sending DANFE, boleto and charges via WhatsApp with an approved template.
- E-commerce: order import (Shopify, WooCommerce, Mercado Livre) via webhook/REST API.
- Contabilidade: entry export in a format compatible with major accounting systems.

### 11.4 Infraestrutura e Segurança

- Backup automático: daily database and fiscal-XML backup; minimum 90-day retention.
- Ambiente de testes: a separate sandbox for training and SEFAZ homologation, with no risk to production.
- Logs de sistema: errors, SEFAZ transmissions, background jobs — accessible to technical support.
- Criptografia: bcrypt passwords; A1 certificates encrypted at rest; HTTPS mandatory.
- Monitoramento: automatic alerts for transmission failures, delayed jobs and critical errors.

## Domain model

- **User** (aggregate root) — `credentials` (bcrypt hash), `profile: Profile` (or a custom combination), `twoFactorEnabled: boolean` (mandatory for admin), `status`.
- **Profile** — one of the standard profiles or a custom one; `permissions: [Permission]`.
- **Permission** — `module`, `screen`, `action: {VIEW, CREATE, EDIT, DELETE, APPROVE, EXPORT}`.
- **AccessLog** — `user`, `event: {LOGIN, LOGOUT}`, `ip`, `device`, `timestamp` — retained 12 months.
- **AuditTrailEntry** (immutable, append-only, written by a DB trigger per doc §13 — never solely application-code-driven) — `user`, `timestamp`, `screen`, `entity`, `field`, `oldValue`, `newValue`.
- **ApprovalAlcada** — shared configuration consumed by `sales`, `purchasing`, `finance`: `module`, `thresholdValue`/`thresholdDiscountPercent`, `approverProfile`.
- **IntegrationCredential** — per external integration (SEFAZ, Receita Federal, ViaCEP/IBGE, banks, WhatsApp Business API, e-commerce platforms, accounting export), holding endpoint/credentials and environment (production/homologation where applicable).
- **BackupJob**, **MonitoringAlert** — infra-facing entities recording backup runs and triggered alerts (transmission failures, delayed jobs, critical errors).

## Use cases (`application.port.in`)

Each use case below has a standalone implementation ticket under [`m10-sistema/`](m10-sistema/README.md). Most ship in Phase 1 (Fundação), since every other module depends on authentication, permissions and approval alçadas; audit-trail querying, SEFAZ monitoring, backup and alerting ship as hardening in Phase 8 (Robustez).

| Use case | Responsibility |
|---|---|
| [`RegisterUserUseCase`](m10-sistema/uc-01-register-user.md) | Create a user with credentials and an assigned profile. |
| [`UpdateUserUseCase`](m10-sistema/uc-02-update-user.md) | Edit a user's data, profile or status. |
| [`AssignProfileUseCase`](m10-sistema/uc-03-assign-profile.md) | Set the permission set of a standard profile. |
| [`SaveCustomProfileUseCase`](m10-sistema/uc-04-save-custom-profile.md) | Save an arbitrary permission combination as a reusable profile. |
| [`AuthenticateUseCase`](m10-sistema/uc-05-authenticate.md) | Login + password check + TOTP 2FA (mandatory for admin). |
| [`CheckPermissionUseCase`](m10-sistema/uc-06-check-permission.md) | The port every other context's REST layer calls before executing an action. |
| [`GetAccessLogUseCase`](m10-sistema/uc-07-get-access-log.md) | Login/logout/IP/device history. |
| [`GetAuditTrailUseCase`](m10-sistema/uc-08-get-audit-trail.md) | Per-record change history, admin-only. |
| [`ConfigureApprovalAlcadaUseCase`](m10-sistema/uc-09-configure-approval-alcada.md) | Shared alçada thresholds consumed by `sales`/`purchasing`/`finance`. |
| [`ConfigureIntegrationCredentialUseCase`](m10-sistema/uc-10-configure-integration-credential.md) | Register/rotate credentials for each external integration. |
| [`MonitorSefazAvailabilityUseCase`](m10-sistema/uc-11-monitor-sefaz-availability.md) | Availability polling for production/homologation SEFAZ endpoints. |
| [`TriggerBackupUseCase`](m10-sistema/uc-12-trigger-backup.md) | Daily automatic DB + fiscal-XML backup, 90-day retention. |
| [`RecordMonitoringAlertUseCase`](m10-sistema/uc-13-record-monitoring-alert.md) | Transmission failures, delayed jobs, critical errors. |

## Outbound ports (`application.port.out`)

| Port | Purpose |
|---|---|
| `UserRepositoryPort`, `ProfileRepositoryPort`, `AccessLogRepositoryPort`, `AuditTrailRepositoryPort`, `ApprovalAlcadaRepositoryPort`, `IntegrationCredentialRepositoryPort` | Persistence. |
| `TotpVerificationPort` | Google Authenticator/Authy-compatible TOTP check. |
| `BackupExecutionPort` | Triggers the DB + XML backup job. |
| `AlertNotificationPort` | Delivers monitoring alerts to technical support. |
| `WhatsAppBusinessApiPort` | Shared integration consumed by M3 (DANFE NFC-e), M7 (quotes), M8 (boleto/charge), M2 (DANFE). |
| `EcommerceOrderImportPort` | Webhook/REST import from Shopify/WooCommerce/Mercado Livre, feeding `sales`. |

## Adapters

### Inbound (`adapter.in.web`)

| Method & path | Use case |
|---|---|
| `POST /api/auth/login`, `POST /api/auth/2fa/verify` | `AuthenticateUseCase` |
| `POST /api/users` / `PATCH /api/users/{id}` | User lifecycle |
| `PUT /api/profiles/{id}/permissions` | `AssignProfileUseCase` / `SaveCustomProfileUseCase` |
| `GET /api/system/access-log` | `GetAccessLogUseCase` |
| `GET /api/system/audit-trail?entity=&id=` | `GetAuditTrailUseCase` |
| `PUT /api/system/alcadas/{module}` | `ConfigureApprovalAlcadaUseCase` |
| `PUT /api/system/integrations/{name}/credentials` | `ConfigureIntegrationCredentialUseCase` |
| `GET /api/system/sefaz-status` | `MonitorSefazAvailabilityUseCase` |
| `POST /api/system/backups/run` | `TriggerBackupUseCase` |
| `GET /api/system/alerts` | `RecordMonitoringAlertUseCase` (query side) |
| `POST /webhooks/ecommerce/{platform}` | `EcommerceOrderImportPort` entry point |

### Outbound (`adapter.out.persistence` / integrations)

`UserJpaEntity`, `ProfileJpaEntity`, `PermissionJpaEntity`, `AccessLogJpaEntity`, `IntegrationCredentialJpaEntity`; `AuditTrailEntry` is written by a **database trigger**, not by an application-layer repository call, per doc §13 ("auditoria por trigger... nunca dependente do código da aplicação"); a TOTP library adapter; a scheduled backup adapter; one adapter per external integration (banks are covered in `finance`'s `BankIntegrationPort`, not duplicated here).

## Cross-module dependencies

- **Provides to every context**: `CheckPermissionUseCase` (authorization gate on every REST endpoint), `AuditTrailEntry` (via DB trigger, applies to every table), `ApprovalAlcadaRepositoryPort` (consumed by `sales`, `purchasing`, `finance`), `WhatsAppBusinessApiPort`, `EcommerceOrderImportPort` (feeds `sales`).
- **Monitors**: `tax`'s SEFAZ transmissions (via `MonitorSefazAvailabilityUseCase` / `RecordMonitoringAlertUseCase`).

## Notes

- `ApprovalAlcada` is centralized here rather than duplicated per module, resolving the ambiguity noted in [m6-compras.md](m6-compras.md), [m7-vendas-crm.md](m7-vendas-crm.md) and [m8-financeiro.md](m8-financeiro.md).
- The doc lists bank integrations under both M10 §11.3 and M8 §9.1/§9.2; this spec keeps the actual `BankIntegrationPort` in `finance` (where the domain behavior lives) and treats M10's mention as the credential/registration side (`IntegrationCredentialRepositoryPort`), to avoid two competing implementations.
