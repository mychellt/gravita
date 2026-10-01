---
name: gravita-web
description: Explores, builds and runs the Angular frontend under web/ (same repo) — its routes, screens, module structure, the data shapes it currently expects, and how to actually launch it locally — and cross-references it against the backend specs in docs/specs/. Use whenever a question or task touches the web UI — its screens, routes, components, frontend data contracts, building/running it — or backend/frontend alignment (e.g. "what does the PDV screen show", "does Customer match what the frontend expects", "run the web app", "align the API with the web app").
---

# Gravita Web — Frontend Understanding

The Angular prototype of the ERP's web interface lives in this same repo,
under `web/` (merged in via `git subtree` from what was previously a separate
`gravita-web` repo — its own commit history is preserved on that path). It's
a fully independent build: its own `package.json`/`angular.json` inside
`web/`, deployed by `.github/workflows/deploy-web-aws.yml` (path-filtered to
`web/**`, `working-directory: web`) — entirely separate from the backend's
Maven build at the repo root.

## What it is

Angular 17, standalone components, signals for state, lazy-loaded routes, SCSS
design tokens via CSS custom properties. It is **UI-only right now**: every
screen reads from `DataService`, which holds hardcoded mock data behind
signals. There is no `HttpClient`, no auth interceptor, no real backend call
anywhere in it yet — wiring it to a real API is explicitly the first "next
step" in its own README. Keep that in mind: nothing you read there describes
an existing API contract, only the shape the UI *expects* one to have.

## Where things live

| Path | What's there |
|---|---|
| `web/README.md` | Stack, folder layout, module→route table — read this first. |
| `web/src/app/app.routes.ts` | The actual route map (lazy `loadComponent` per module). Cross-check against the README table below; it can drift. |
| `web/src/app/core/models/index.ts` | Every domain interface the UI works with (`Empresa`, `Cliente`, `Fornecedor`, `Produto`, `PedidoVenda`, `TituloFinanceiro`, etc.) — the frontend's implicit data contract. |
| `web/src/app/core/services/data.service.ts` | Mock data + signals simulating backend state; shows what a real API response would need to look like. |
| `web/src/app/layout/{shell,sidebar,topbar}/` | App chrome — not module-specific. |
| `web/src/app/modules/<name>/` | One folder per screen/module (`.ts` + `.html` + `.scss`); `nfe/nfe-form/` is a nested sub-screen. |
| `web/src/app/shared/{components,pipes}/` | Reusable UI: `badge`, `page-header`, `toast`, `brl.pipe` (BRL currency formatting). |
| `web/public/index.html` | A **separate static marketing/landing page** (hero, pricing, FAQ, `register.html` signup flow) — not the Angular app. See below. |
| `.github/workflows/deploy-web-aws.yml` | CI: builds and deploys `web/` to S3, triggered only on `web/**` changes. |

## Two sites in one build — don't confuse them

`web/angular.json`'s `build.options.index` sets `input: src/index.html,
output: app.html` — the real Angular SPA is built to **`app.html`**, not
`index.html`. Meanwhile `web/public/index.html` (a separate, static,
hand-built marketing page — hero section, pricing, FAQ, `register.html` for
signup) is copied verbatim to the build output root as `assets`, so it ends
up occupying `index.html`. Net effect, both in `ng serve` and the built
`dist/gravita-web/`:

- `/` → the static marketing page (`public/index.html`) — not part of the
  Angular app, has its own `app.js`/`styles.css`, nothing here reflects
  `docs/specs/`.
- `/app.html`, and any Angular route (`/dashboard`, `/pdv`, `/nfe`, …) → the
  actual ERP SPA (`src/index.html` → `<app-root>` → `app.routes.ts`).

If you navigate to `localhost:4200` and see a marketing hero with "Começar
grátis" instead of a sidebar and dashboard, you're on the landing page — go
to `/app.html` (or any route from the table below) for the real app.

## Module → route → backend module map

From the README, current as of last read — verify against `web/src/app/app.routes.ts` if
it's been a while:

| Route | Component | Backend module (this repo's `docs/specs/`) |
|---|---|---|
| `/dashboard` | `dashboard/` | M9 — [m9-relatorios-bi.md](../../../docs/specs/m9-relatorios-bi.md) |
| `/pdv` | `pdv/` | M3 — [m3-fiscal-nfce-pdv.md](../../../docs/specs/m3-fiscal-nfce-pdv.md) |
| `/nfe`, `/nfe/nova` | `nfe/`, `nfe/nfe-form/` | M2 — [m2-fiscal-nfe.md](../../../docs/specs/m2-fiscal-nfe.md) |
| `/nfse` | `nfse/` | M4 — [m4-fiscal-nfse.md](../../../docs/specs/m4-fiscal-nfse.md) |
| `/inventory` | `inventory/` | M5 — [m5-estoque.md](../../../docs/specs/m5-estoque.md) |
| `/purchasing` | `purchasing/` | M6 — [m6-compras.md](../../../docs/specs/m6-compras.md) |
| `/crm` | `crm/` | M7 — [m7-vendas-crm.md](../../../docs/specs/m7-vendas-crm.md) |
| `/finance` | `finance/` | M8 — [m8-financeiro.md](../../../docs/specs/m8-financeiro.md) |
| `/reports` | `reports/` | M9 — [m9-relatorios-bi.md](../../../docs/specs/m9-relatorios-bi.md) |
| `/settings` | `settings/` | M10 — [m10-sistema.md](../../../docs/specs/m10-sistema.md) |
| — (no route yet) | — | M1 — [m1-cadastros-base.md](../../../docs/specs/m1-cadastros-base.md): the frontend has `Cliente`/`Fornecedor`/`Empresa` models but no dedicated cadastro screens visible in the route list; check `settings/` and each module's forms before assuming it's entirely missing. |

## Cross-referencing with the backend specs

This is the highest-value use of this skill: `web/src/app/core/models/index.ts`'s
interfaces map closely (often 1:1) onto the aggregates in `docs/specs/mX-*.md`
and their `uc-NN-*.md` tickets, but the naming is independently derived —
Portuguese field names here vs. English in the specs. Known correspondences,
confirmed by reading both:

- `Empresa` ↔ M1 `Company` (`regime` ↔ `taxRegime`, `ambiente` ↔ `sefazEnvironment`)
- `Cliente` ↔ M1 `Customer` (`indicadorIe` ↔ `ieIndicator`, `saldoDevedor` ↔ `currentBalance`, `limiteCredito` ↔ `creditLimit`)
- `Fornecedor` ↔ M1 `Supplier`
- Status string unions (`NFeStatus`, `PedidoStatus`, `PedidoCompraStatus`, `TituloStatus`, `FunilEstagio`, `MovimentoTipo`, …) ↔ the `status: {...}` enums on the matching backend aggregate — compare values directly, they don't always line up (e.g. frontend `PedidoCompraStatus` has `aguarda_aprovacao`/`em_transito` that may not exist yet in the M6 spec — flag mismatches like this rather than silently picking one side).

When asked to align backend API design with the frontend, or to check whether
a spec/ticket matches what the UI needs, read the relevant interface(s) in
`web/src/app/core/models/index.ts` side by side with the matching module spec's "Domain
model" section, and call out every field that doesn't have an obvious match
in either direction — don't assume the frontend mock is authoritative (it's a
prototype guess) or that the backend spec is complete (it's derived from a
functional doc, not from this frontend).

## Building and running it locally

```bash
cd web
npm ci            # first time, or after package.json changes
npm run build     # sanity-check: production bundle compiles, no errors
npm start &        # ng serve — http://localhost:4200 (backgrounded; see below)
```

Then open `/app.html` (or any Angular route) in a browser — not bare `/`, see
above. Poll instead of guessing when it's ready:
`timeout 60 bash -c 'until curl -sf http://localhost:4200 >/dev/null; do sleep 1; done'`.

**Gotcha that recurs — a stale server squats the port.** Before starting,
check nothing already owns 4200 and confirm *whose* it is before trusting it:

```bash
lsof -ti:4200 -sTCP:LISTEN                              # PID, if any
readlink -f /proc/<that PID>/cwd                         # where it's rooted
```

This repo used to be two separate checkouts (`gravita` and `gravita-web`,
before the `git subtree` merge into `web/`); the old standalone
`~/dev/gravita-web` directory can still exist on disk with its own orphaned
`ng serve` left running from before the merge. If the cwd isn't
`.../gravita/web`, it's not this repo's server — `ng serve` will otherwise
hang at an unanswerable interactive "port in use, try another? (Y/n)" prompt
when backgrounded, and curling/opening the port will silently show you the
*stale* build instead of what you just changed. Kill the wrong PID, not the
port blindly, then start fresh. Stop your own server the same way:
`lsof -ti:4200 -sTCP:LISTEN | xargs kill`.

## How to explore

- **Narrow question** ("what does the PDV screen show", "what fields does the
  NFe form collect") — just read the 1-3 relevant files directly
  (`web/src/app/modules/<name>/<name>.component.ts`/`.html`, plus the matching
  interface in `web/src/app/core/models/index.ts`). No need to survey the
  whole `web/` tree.
- **Broad survey** ("understand the whole web interface", "audit frontend vs.
  backend for all modules") — delegate to a subagent (`general-purpose`, or a
  `fork` if you already have gravita backend spec context loaded in this
  session that the subagent would otherwise have to re-derive) so the raw
  file contents don't fill this session's context. Ask it to report back a
  structured, per-module summary — not a dump of every file it read.
- Either way, report findings concisely: which screens/fields exist, what
  data shape they expect, and any concrete mismatch against `docs/specs/`
  worth flagging — not a transcript of the files themselves.
- `web/node_modules/`, `web/dist/` and `web/.angular/` are build artifacts,
  gitignored, and never worth reading.
