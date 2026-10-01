---
name: clean-code
description: Clean code guidelines for the Gravita project (Java/Spring Boot backend and Angular frontend under web/). Use when writing, refactoring or reviewing code — naming, function size, duplication, comments, error handling, and keeping changes small and readable.
---

# Clean Code

Apply these when writing or reviewing code. Match the surrounding code's style first; these rules break ties.

## Naming
- Names reveal intent: `overdueInvoices`, not `list2`. No abbreviations unless domain-standard.
- Classes/types are nouns; methods/functions are verbs. Booleans read as questions (`isActive`, `hasStock`).
- One word per concept across the codebase (don't mix `fetch`/`get`/`load` for the same thing).
- Use the domain's vocabulary (see `docs/specs/`) rather than technical filler (`Manager`, `Helper`, `Util`, `Data`).

## Functions / methods
- Do one thing, at one level of abstraction. Aim for under ~20 lines.
- Few parameters (≤3). Group related ones into a value object/record/interface.
- No boolean flag parameters that switch behavior — split into two functions.
- Prefer early returns / guard clauses over deep nesting.
- Avoid side effects hidden behind query-sounding names.

## Structure
- Single responsibility per class/component/service. If describing it needs "and", split it.
- Keep layers clean: Java — controller → service → repository, no business logic in controllers or entities' persistence glue; Angular — components render, services hold data/logic, no HTTP in templates.
- Depend on abstractions at boundaries; inject dependencies (constructor injection in Spring, `inject()`/constructor in Angular).
- Prefer composition over inheritance.
- Don't add abstractions until there's a second real use (rule of three). Remove dead code and unused imports.

## Duplication
- Extract repeated logic once it appears a third time, or when the copies must change together.
- Don't DRY away code that only looks similar but changes for different reasons.

## Comments
- Code explains *what*; comments explain *why* (constraints, trade-offs, non-obvious business rules).
- Delete commented-out code and stale TODOs; git remembers.

## Error handling
- Fail fast with specific, meaningful exceptions/errors; never swallow them silently.
- Validate at boundaries (API input, form input); trust internal invariants.
- Don't return `null` for "nothing" — use `Optional` (Java) / explicit types (TypeScript, strict null checks).

## Data & state
- Prefer immutability: `record`s and `final` in Java, `readonly` and signals in Angular.
- No magic numbers/strings — name constants or enums.
- Money uses `BigDecimal` (Java) — never floating point.

## Tests
- Readable, one behavior per test, named by behavior (`rejectsOrderWhenStockIsInsufficient`).
- Arrange / Act / Assert; no logic in tests; test behavior, not implementation details.

## Change hygiene
- Keep diffs focused: no drive-by reformatting or unrelated refactors mixed with a feature.
- Leave the code a little cleaner than you found it, but only in the area you're touching.
- Before finishing: remove debug output, unused code, and confirm build/tests pass.