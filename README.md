# Gravita

Gravita is a generic ERP for Brazilian retail: one codebase serving several segments and tax profiles
(Simples Nacional, Lucro Presumido, Lucro Real) with every variable business rule — CFOP, CSOSN, rates,
tax regime — parameterized in registration data instead of in code.

The repository is a monorepo:

| Part | Where | Stack |
|------|-------|-------|
| Backend (REST API) | repo root (`pom.xml`, `src/`) | Java 25, Spring Boot 4.1, PostgreSQL, Flyway, AWS SQS |
| Frontend | [`web/`](web/README.md) | Angular 17 (standalone components, signals) |
| Infrastructure | [`infra/`](infra/README.md) | Terraform, Docker Compose, Caddy, AWS (homologation) |
| Documentation | [`docs/`](docs/) | Architecture notes, functional specs and per-use-case tickets |

The two builds are independent: `mvn` at the root, `npm`/`ng` inside `web/`.

## Modules

The product is organized into ten modules (M1–M10, see [`docs/specs/00-overview.md`](docs/specs/00-overview.md)).
Backend code is grouped by business context:

| Module | Context | REST prefixes (`/api/...`) |
|--------|---------|----------------------------|
| M1 — Master data (companies, customers, suppliers, products, price tables, auxiliary tables) | `masterdata` | `companies`, `customers`, `suppliers`, `products`, `price-tables`, `lookup`, `auxiliary/*` |
| M2 — Fiscal NF-e | `tax` | `nfe`, `sped/fiscal`, `sped/contribuicoes`, `livros-fiscais`, `accounting/export` |
| M3 — Fiscal NFC-e + PDV | `tax` | `pdv/sessions`, `pdv/sales`, `pdv/products`, `pdv/cash-movements` |
| M4 — Fiscal NFS-e | `tax` | `nfse`, `municipalities`, `nfse/discrimination-templates` |
| M5 — Inventory | `inventory` | `inventory/{balances,movements,adjustments,transfers,reservations,counts,lots,alerts,summary,products}` |
| M6 — Purchasing | `purchasing` | `purchasing/{requests,quotations,orders,receipts}` |
| M7 — Sales & CRM | `sales` | `sales/{quotes,orders,commissions}`, `crm/{opportunities,funnel,follow-ups,follow-up-rules,targets}` |
| M8 — Finance | `finance` | `finance/{receivables,payables,customers,cash-flow,bank-statements,internal-cash}` |
| M9 — Reports & BI | `reporting` | `reports/{dashboard,dre,abc-curve,commissions,stock-turnover,purchases-by-supplier,assessed-taxes,fiscal-books}`, `reports/{reportId}/export` |
| M10 — System | `system` | `auth`, `signup`, `activate`, `auth/password-reset`, `users`, `profiles`, `plans`, `system/{alcadas,integrations,access-log}`, `admin/customers` |

Each module has a functional spec (`docs/specs/mN-*.md`) and one implementation ticket per use case
(`docs/specs/mN-*/uc-NN-*.md`). The original functional document is
[`docs/ERP MVP formato DOC.pdf`](docs/ERP%20MVP%20formato%20DOC.pdf).

## Architecture

The backend follows **hexagonal architecture (ports & adapters)** with package-by-feature contexts:

```
br.gravita
├── core/                        # the hexagon — no framework or adapter knowledge
│   ├── domain/<context>/        # entities, value objects, invariants (pure Java)
│   ├── ports/
│   │   ├── inbound/<context>/   # what the core offers
│   │   ├── business/            # use-case ports
│   │   ├── outbound/            # what the core needs (persistence, security, mail, ...)
│   │   ├── integration/         # external systems
│   │   └── messaging/           # queues
│   ├── usercases/<context>/     # use-case implementations (@UseCase)
│   └── annotations/             # @UseCase, @PersistenceAdapter
└── adapters/
    ├── inbound/                 # REST controllers, SQS consumers, schedulers
    ├── outbound/                # JPA persistence, SEFAZ/bank/CNPJ/CEP integrations, mail, SQS,
    │                            # security (ciphers, TOTP, session store), PDF/XLS rendering
    ├── dtos/                    # request/response transport objects
    └── configuration/           # Spring wiring (async, mail, SQS, MVC, seeders)
```

Dependencies point inward only: `adapters → core.usercases → core.ports → core.domain`. The rule is
enforced by an ArchUnit test (`src/test/java/br/gravita/architecture/HexagonalArchitectureTest.java`)
that runs with every `./mvnw test`. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the rationale
(note: its package diagram predates the current `core/` + `adapters/` layout; the tree above is current).

### Cross-cutting behavior

- **Authentication** — `POST /api/auth/login` (with optional TOTP two-factor) returns a session token;
  clients send `Authorization: Bearer <token>`. Sessions are held in memory
  (`InMemorySessionStoreAdapter`). Users have profiles with permissions; a dedicated
  `GRAVITA_ADMINISTRATOR` profile is confined to the administrative area (`/api/admin/*`).
- **Self-service onboarding** — sign-up, e-mail activation link and password reset, delivered through
  SQS consumers that send e-mail (FreeMarker templates in `src/main/resources/mails/templates`).
- **Fiscal transmission queue** — a scheduled consumer polls the transmission queue (default every 30 s)
  to send documents to SEFAZ, with retry and offline contingency.
- **Secrets at rest** — integration credentials and digital certificates are encrypted
  (`CredentialCipher`, `CertificateCipher`).
- **Scheduled jobs** — CRM follow-up rule evaluation, bank return import, batch payment confirmation and
  Pix charge expiration; each has a configurable cron property (e.g. `gravita.finance.bank-return.import-cron`).

## Getting started

### Prerequisites

- JDK 25
- Docker (PostgreSQL, LocalStack and MailHog for local development)
- Node ≥ 18 and npm ≥ 9 (frontend only)

### 1. Start the local dependencies

```bash
make docker-compose-up
```

This starts, via [`docker-compose.yaml`](docker-compose.yaml):

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL 15 | 5432 | database `gravita_db` (user/password `gravita`) |
| LocalStack | 4566 | SQS queues, created by `localstack/init/ready.d` |
| MailHog | 1025 (SMTP), 8025 (UI) | catches outgoing e-mail — open http://localhost:8025 |

LocalStack needs a `LOCALSTACK_AUTH_TOKEN`; set your own in `docker-compose.yaml` if the bundled one is
not valid for you. Tear everything down (including volumes) with `make docker-compose-down`.

### 2. Run the backend

```bash
SPRING_PROFILES_ACTIVE=local \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/gravita_db \
SPRING_DATASOURCE_USERNAME=gravita \
SPRING_DATASOURCE_PASSWORD=gravita \
SQS_USER_REGISTRATION_QUEUE_NAME=user_registration_queue \
./mvnw spring-boot:run
```

The API listens on http://localhost:8080. The `local` profile enables Flyway
(`db/migration/local`), points SQS at LocalStack, uses development encryption keys and seeds the default
plans when the catalog is empty. Without `spring.datasource.*` overrides the app expects a database named
`gravita` on `localhost:5432` with `postgres`/`postgres`.

### 3. Run the frontend

```bash
cd web
npm install
npm start          # http://localhost:4200, proxies /api to localhost:8080
```

See [`web/README.md`](web/README.md) for the frontend structure, routes and S3 deployment.

### API collections

Ready-made request collections live in [`collections/`](collections) for
[Postman](collections/postman/gravita-api.postman_collection.json) and
[Insomnia](collections/insomnia/gravita-api.insomnia.json).

## Configuration

Profiles: `local` (development), `hom` (homologation) and `prod` (production). `hom` and `prod` have
Flyway disabled and read everything below from the environment.

| Variable | Used for |
|----------|----------|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | PostgreSQL connection |
| `INTEGRATION_CREDENTIAL_ENCRYPTION_KEY` | encryption key for integration credentials (base64, 32 bytes) |
| `CERTIFICATE_ENCRYPTION_KEY` | encryption key for digital certificates (base64, 32 bytes) |
| `PDV_SUPERVISOR_PASSWORD_HASH` | bcrypt hash of the PDV supervisor password |
| `SQS_USER_REGISTRATION_QUEUE_NAME`, `SQS_PASSWORD_RESET_QUEUE_NAME` | SQS queues consumed by the app |
| `AWS_SQS_EMAIL_QUEUE_URL`, `AWS_SQS_WHATSAPP_QUEUE_URL` | SQS queues the app publishes to |
| `SPRING_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` / `_SMTP_AUTH`, `APP_MAIL_SENDER` | SMTP (defaults point at MailHog) |
| `ACTIVATION_LINK_BASE_URL`, `ACTIVATION_RESULT_PAGE_URL`, `PASSWORD_RESET_LINK_BASE_URL` | links placed in activation / reset e-mails |
| `PURCHASING_APPROVER_EMAIL`, `INVENTORY_EXPIRING_LOTS_EMAIL`, `CRM_FOLLOW_UP_EMAIL`, `FINANCE_CASH_FLOW_ALERT_EMAIL` | notification recipients |

Generate encryption keys with `openssl rand -base64 32`; never reuse the `local` profile's keys elsewhere.

## Build and test

```bash
./mvnw test        # unit + architecture tests (ArchUnit); Checkstyle (checkstyle.xml) runs first and fails the build on violations
./mvnw verify      # full build, including JaCoCo coverage
./mvnw package     # executable jar in target/
```

Frontend: `cd web && npm test` (Karma + Chrome headless) and `npm run build:prod`.

Database migrations are in `src/main/resources/db/migration/{local,hom,prod}`.

## Docker image

The [`Dockerfile`](Dockerfile) builds the jar with Temurin JDK 25 and runs it on `eclipse-temurin:25-jre-alpine`
as a non-root user with `SPRING_PROFILES_ACTIVE=prod`, exposing port 8080.

## Deployment and CI

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| `deploy-homolog.yml` | push to `homolog` | runs backend tests, builds the `linux/arm64` image and the Angular app, deploys to the homologation VM through SSM (no SSH, no stored AWS keys) |
| `deploy-web-aws.yml` | push to `master` touching `web/**` | builds the Angular app and publishes it to S3 |
| `infra-ci.yml` | changes under `infra/**` | `terraform fmt`/`validate`, shellcheck, compose config, SOPS check |

Homologation runs on a single AWS EC2 Graviton instance (Docker Compose + Caddy for TLS) with SQS queues
and an S3 backup bucket, all defined in Terraform. Provisioning, secrets (SOPS), manual deploys, backups
and restores are documented in [`infra/README.md`](infra/README.md).

## Documentation map

- [`docs/specs/00-overview.md`](docs/specs/00-overview.md) — product principles, module map, roadmap phases
- [`docs/specs/11-ux-e-decisoes-tecnicas.md`](docs/specs/11-ux-e-decisoes-tecnicas.md) — UX rules and technical decisions
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — hexagonal architecture background
- [`docs/erp-operations-story.md`](docs/erp-operations-story.md) — a narrative walkthrough of ERP operations
- [`web/README.md`](web/README.md) and [`infra/README.md`](infra/README.md) — frontend and infrastructure guides
