# UC-M3-04 — Issue NFCe (`IssueNfceUseCase`)

**Module:** M3 — Fiscal: NFCe + PDV ([module spec](../m3-fiscal-nfce-pdv.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§4.2: "Online mode: NFC-e issued and authorized in real time before printing; average time < 3s." "Contingency mode: offline sale numbered with a pending flag; automatic sync on reconnect."

## Description

Takes the `DRAFT` `NfceSale` from [UC-03](uc-03-register-nfce-sale.md), calculates taxes via M2's shared `CalculateTaxUseCase`, allocates a document number, and attempts real-time SEFAZ authorization. If SEFAZ-UF is unavailable, the sale is queued in contingency mode instead of blocking the cashier. On success the sale is `AUTHORIZED` and ready for DANFE NFC-e printing/delivery.

## Port signature

```java
public interface IssueNfceUseCase {
    NfceIssuanceResult execute(IssueNfceCommand command);
}
```

`IssueNfceCommand`: `nfceSaleId`. Returns `NfceIssuanceResult { status, accessKey, protocol (if authorized) }`.

## Outbound ports required

- `TransmissionQueuePort`, `SubmitToSefazPort` (shared with M2)
- `AllocateDocumentNumberUseCase` (from `masterdata`, called before queuing — per the module spec's cross-module dependency on numbering allocation)
- `PosSessionRepositoryPort`, `CompanyRepositoryPort` — the issuing `Company` is resolved via the sale's `PosSession.companyId` (GRA-96); `originState`/`destinationState` passed to `CalculateTaxUseCase` are both that company's `state` (NFC-e is always same-state).

## REST endpoint

`POST /api/pdv/sales` (shared with [UC-03](uc-03-register-nfce-sale.md) per the module spec's adapter table: register, then issue, behind one endpoint).

## Domain entities touched

- `NfceSale` (status transition `DRAFT → AUTHORIZED` or `DRAFT → PENDING_SYNC`)

## Acceptance criteria

- [ ] Online mode: NFC-e is authorized in real time before printing, average time under 3 seconds.
- [ ] If SEFAZ-UF is unavailable, the sale is queued with `contingencyMode = true` and `status = PENDING_SYNC`, without blocking the cashier.
- [ ] The sale's tax totals come from `CalculateTaxUseCase` — never recomputed locally in M3.
- [ ] Document numbering is allocated exactly once per sale, with no duplicates under concurrent issuance.

## Dependencies

- **Depends on:** [03 — Register Sale](uc-03-register-nfce-sale.md); M2 — `CalculateTaxUseCase` (../m2-fiscal-nfe/uc-02-calculate-tax.md); `masterdata` numbering allocation.
- **Blocks:** DANFE NFC-e printing/delivery, [07 — Cancel NFCe](uc-07-cancel-nfce.md), [08 — Sync Contingency Sales](uc-08-sync-contingency-sales.md), [09 — Void Untransmitted Numbering](uc-09-void-untransmitted-numbering.md).
