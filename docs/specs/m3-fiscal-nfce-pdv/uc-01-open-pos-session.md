# UC-M3-01 — Open POS Session (`OpenPosSessionUseCase`)

**Module:** M3 — Fiscal: NFCe + PDV ([module spec](../m3-fiscal-nfce-pdv.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§4.1: "Cash opening: initial change amount; operator linked to a physical register." §4.1: "Multiple registers: each with its own operator; consolidated at day closing."

## Description

Triggered when a cashier operator starts their shift on a physical register. The operator informs the initial change (troco) amount; the system opens a session tying that operator to that register for every subsequent PDV action, until closed.

## Port signature

```java
public interface OpenPosSessionUseCase {
    PosSessionId execute(OpenPosSessionCommand command);
}
```

`OpenPosSessionCommand`: `registerId`, `operatorId`, `companyId` (the issuing company/branch for every sale under this session — GRA-96), `openingChangeAmount: Money`. Returns the id of the created `PosSession`.

## Outbound ports required

- `PosSessionRepositoryPort`
- `CompanyRepositoryPort` — validates `companyId` refers to an existing `Company`/branch (GRA-96).

## REST endpoint

`POST /api/pdv/sessions`

## Domain entities touched

- `PosSession` (created, status `OPEN`)

## Acceptance criteria

- [ ] Opening a session on a `registerId` that already has an `OPEN` session is rejected (invariant: one open session per physical register).
- [ ] Session is created with `status = OPEN`, `openedAt` set, and the given `openingChangeAmount`.
- [ ] The operator is linked to the register for the session's lifetime.
- [ ] Multiple registers can each hold an independent open session concurrently.

## Dependencies

- **Depends on:** None.
- **Blocks:** [02 — Search Product](uc-02-search-product-for-sale.md), [03 — Register Sale](uc-03-register-nfce-sale.md), [05 — Cash Movement](uc-05-record-cash-movement.md), [06 — Close Session](uc-06-close-pos-session.md) — every PDV action requires an open session.
