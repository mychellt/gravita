# UC-M3-06 — Close POS Session (`ClosePosSessionUseCase`)

**Module:** M3 — Fiscal: NFCe + PDV ([module spec](../m3-fiscal-nfce-pdv.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§4.1: "Cash closing: reconciliation by payment method; Z report printed or saved as PDF." "Multiple registers: each with its own operator; consolidated at day closing."

## Description

Ends the cashier's shift on a register. The system reconciles totals by payment method against the sales and cash movements recorded during the session, generates the `CashClosingReport` (Z report), and closes the session so no further PDV actions can target it.

## Port signature

```java
public interface ClosePosSessionUseCase {
    CashClosingReport execute(ClosePosSessionCommand command);
}
```

`ClosePosSessionCommand`: `sessionId`, `closingCountedAmounts` (optional, by payment method, for physical-count reconciliation). Returns the generated `CashClosingReport`.

## Outbound ports required

- `PosSessionRepositoryPort`
- `NfceRepositoryPort` (to total the session's sales)
- `CashMovementRepositoryPort` (to include sangria/suprimento in the reconciliation)
- `PrintNonFiscalReceiptPort` (Z report print or PDF)

## REST endpoint

`POST /api/pdv/sessions/{id}/close`; report retrieval via `GET /api/pdv/sessions/{id}/z-report`.

## Domain entities touched

- `PosSession` (status `OPEN → CLOSED`), `CashClosingReport` (generated), reads `NfceSale` and `CashMovement`.

## Acceptance criteria

- [ ] Reconciliation totals are broken down by payment method.
- [ ] The Z report includes opening amount, closing amount, cash movements and sale count.
- [ ] The Z report can be printed on the non-fiscal printer or saved as PDF.
- [ ] The session transitions to `CLOSED`; no further sales or cash movements are accepted against it.
- [ ] Multiple registers' closings can be consolidated into a single day closing.

## Dependencies

- **Depends on:** [01 — Open Session](uc-01-open-pos-session.md); reads the output of [03](uc-03-register-nfce-sale.md)/[04](uc-04-issue-nfce.md) (sales) and [05](uc-05-record-cash-movement.md) (cash movements).
- **Blocks:** Day-level cash consolidation (cross-module — `finance` §9.3, Caixa interno).
