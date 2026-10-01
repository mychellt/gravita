---
name: hexagonal-architecture
description: Hexagonal (ports & adapters) architecture rules for the Gravita Spring Boot backend — where code goes (core/domain, core/ports, core/usercases, adapters), dependency direction, how to add a use case, port or adapter, and the ArchUnit tests that enforce it. Use when adding or moving backend Java code, designing a new feature/module, or reviewing backend structure.
---

# Hexagonal Architecture — Gravita backend

Backend lives at the repo root (`src/main/java/br/gravita`). The frontend (`web/`) is out of scope. `docs/ARCHITECTURE.md` has background, but its package diagram is stale — **the real layout is below; trust the code.**

## Layout

```
br.gravita
├── core/                       # inside the hexagon — no adapter/framework knowledge
│   ├── domain/<context>/       # entities, value objects, invariants (pure Java)
│   ├── ports/
│   │   ├── inbound/<context>/  # driving ports: what the core offers
│   │   ├── business/           # use-case ports (Create…Port, Find…Port, List…Port, Delete…Port)
│   │   ├── outbound/           # driven ports: what the core needs (repos, etc.)
│   │   ├── integration/        # driven ports for external systems
│   │   └── messaging/          # driven ports for messaging
│   ├── usercases/<context>/    # use-case implementations, annotated @UseCase
│   └── annotations/            # @UseCase, @PersistenceAdapter (aliases over @Component)
└── adapters/                   # outside the hexagon
    ├── inbound/controllers/    # REST controllers (driving), scheduling/
    ├── outbound/               # persistence, integration, messaging, security, rendering, reporting
    ├── dtos/{request,response} # transport objects, never leak into core
    └── configuration/          # Spring wiring
```

Contexts (package-by-feature): masterdata, tax, inventory, purchasing, sales, finance, reporting, system.

## Dependency rule

`adapters → core.usercases → core.ports → core.domain`. Never the reverse.

- **domain**: pure Java. No Spring, `jakarta.persistence`, `jakarta.validation`, no ports/usercases/adapters.
- **core (ports, usercases)**: may not import `adapters..`, nor web/JPA types. Talks to the world only through ports.
- **adapters**: may use frameworks freely; translate between transport/persistence models and domain.

Enforced by `src/test/java/br/gravita/architecture/HexagonalArchitectureTest.java` (ArchUnit, runs in `./mvnw test`). Run it after structural changes. Note its patterns match both old (`application`/`adapter`) and current (`core`/`adapters`) names.

## Adding a feature (inside-out)

1. **Domain** — model/extend the entity or value object in `core/domain/<context>`; put invariants in it and throw domain exceptions (e.g. the shared `BusinessRuleException`).
2. **Port** — define the use case interface (`XxxPort`) in `core/ports/business` (or `inbound/<context>`), and any needed driven port in `core/ports/outbound|integration|messaging`. Name by capability, one use case per port.
3. **Use case** — implement in `core/usercases/<context>`, annotate `@UseCase`, constructor-inject only ports. Orchestrate; don't put business rules here that belong in the domain.
4. **Adapters** — controller in `adapters/inbound/controllers/<context>` (maps DTO ↔ domain, calls the use-case port); persistence/integration adapter in `adapters/outbound/...` implementing the outbound port (`@PersistenceAdapter` for persistence). JPA entities stay in the adapter with a mapper to domain.
5. **Test** — unit-test domain and use cases with fake/mock ports (no Spring context); adapter tests separately.

Don't scaffold empty packages ahead of need.

## Rules of thumb

- Controllers depend on the **port interface**, never on a use-case class or repository.
- Domain objects are not returned directly over HTTP — map to `adapters/dtos/response`.
- No JPA annotations, Spring `@Transactional` aside from where the project already places it (use-case or adapter boundary — follow existing code), or `ResponseEntity` in the core.
- Variation by external provider (e.g. NFSe standards ABRASF/National) = one outbound port, several adapter implementations selected in `adapters/configuration`.
- Cross-context calls go through the other context's inbound port, not its domain internals or repositories.
- Shared value objects used by several contexts live in `core/domain/shared`.
- When unsure where a class goes, ask: "does it know about HTTP, SQL, or a vendor SDK?" → adapter. "Does it describe a business rule?" → domain. "Does it coordinate steps?" → use case.