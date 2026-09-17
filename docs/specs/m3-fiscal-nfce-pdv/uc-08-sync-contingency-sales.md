# UC-M3-08 — Sync Contingency Sales (`SyncContingencySalesUseCase`)

**Module:** M3 — Fiscal: NFCe + PDV ([module spec](../m3-fiscal-nfce-pdv.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 8 — Robustez (doc §14)

## Functional requirement

§4.2: "Contingency mode: offline sale numbered with a pending flag; automatic sync on reconnect."

## Description

Background process that runs once connectivity to SEFAZ is restored after an outage. It picks up every sale left in `PENDING_SYNC` (issued in contingency mode by [UC-04](uc-04-issue-nfce.md)) and (re)transmits it for authorization, without cashier intervention.

## Port signature

```java
public interface SyncContingencySalesUseCase {
    SyncResult execute();
}
```

No user-supplied command; optionally scoped to a `registerId`. Returns `SyncResult { syncedCount, failedCount }`.

## Outbound ports required

- `TransmissionQueuePort`, `SubmitToSefazPort` (shared with M2)
- `NfceRepositoryPort`

## REST endpoint

None — this is a scheduler/connectivity-restored-event-triggered background job, not a synchronous user-facing endpoint. The module's inbound adapter table doesn't list one; inferred from "automatic sync on reconnect" (§4.2).

## Domain entities touched

- `NfceSale` (status `PENDING_SYNC → AUTHORIZED` or `PENDING_SYNC → REJECTED`)

## Acceptance criteria

- [ ] Sync triggers automatically on reconnect, with no cashier action required.
- [ ] Every `PENDING_SYNC` sale for the affected register(s) is retransmitted.
- [ ] Successful sales transition to `AUTHORIZED`; failures stay queued for retry with exponential backoff (doc §13).
- [ ] No sale is ever transmitted twice as a duplicate fiscal document.

## Dependencies

- **Depends on:** [04 — Issue NFCe](uc-04-issue-nfce.md), which produces `PENDING_SYNC` sales.
- **Blocks:** —
