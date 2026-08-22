# Hexagonal Architecture — Gravita

The project follows hexagonal architecture (ports & adapters) combined with
package-by-feature: each business context of the ERP (see `docs/ERP MVP
formato DOC.pdf`, module map M1–M10) is a top-level package under
`br.gravita` with the same internal three-layer shape.

```
br.gravita.<context>/
├── domain/           pure business rules — no Spring, no JPA, no HTTP
│   └── model/        entities, value objects, invariants
├── application/       use cases — orchestrates the domain, knows nothing about adapters
│   ├── port/
│   │   ├── in/       what the context offers (use case interfaces)
│   │   └── out/      what the context needs (persistence, integrations)
│   └── service/      use case implementations (@UseCase)
└── adapter/
    ├── in/web/        driving adapter — REST controllers, DTOs
    └── out/persistence/  driven adapter — JPA entities, repositories, mapper
```

`br.gravita.shared` is the shared kernel: value objects used by more than one
context (`Document`, `PersonType`), the base domain exception
(`BusinessRuleException`), and the `@UseCase` / `@PersistenceAdapter`
stereotypes — thin aliases over `@Component` so the application layer reads
in domain vocabulary, not Spring's.

`br.gravita.masterdata` (M1 — Master Data) is the living reference for the
pattern, with the full customer registration flow implemented end to end.

## Dependency rules (enforced)

`HexagonalArchitectureTest` (ArchUnit, runs on every `mvn test`) guarantees,
for **any** context under `br.gravita`:

- `domain` never depends on `application` or `adapter`.
- `domain` never depends on Spring, `jakarta.persistence` or `jakarta.validation`.
- `application` never depends on `adapter`, nor directly on Spring Web/JPA.
- Only `adapter` may depend on frameworks freely.

Because the rule uses `resideInAPackage("..domain..")` etc., it automatically
applies to any new context — there's no need to touch the test when creating
`tax`, `inventory`, `purchasing`, `sales`, `finance`, `reporting` or `system`
(modules M2–M10 of the MVP).

## Convention for a new context

When starting a new module (e.g. M5 Inventory), replicate the shape of
`masterdata`: model the aggregate in `domain.model`, define the use cases in
`application.port.in`/`out` + `application.service`, and only then write the
adapters. Don't scaffold empty packages ahead of time — create the structure
when the first real use case exists.

One decision specific to the tax domain (M2/M3/M4, doc §13) is worth noting
now: the "single tax engine" and the per-standard NFSe adapter (ABRASF,
National, ISS.net...) are examples of an outbound port
(`IssueNfsePort`) with multiple interchangeable implementations — the same
ports & adapters pattern applied to *external-provider variation*, not just
to technology swapping.
