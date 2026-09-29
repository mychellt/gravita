# UC-M8-05 — Import Bank Return (`ImportBankReturnUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.1 Contas a Receber — "Baixa automática: retorno bancário (CNAB 240/400) importado diariamente; conciliação automática."

## Description

Scheduled/daily job that imports a CNAB 240/400 return file from each configured bank, matches each line to an open `Receivable` (by title identifier), and automatically creates a `Settlement` (`method = AUTOMATIC_CNAB`) for matched, paid titles.

## Port signature

```java
public interface ImportBankReturnUseCase {
    BankReturnImportResult execute(ImportBankReturnCommand command);
}
```

`ImportBankReturnCommand`: `bankIntegration`, `fileContent` (CNAB 240/400 payload). Returns a `BankReturnImportResult` (matched/settled count, unmatched lines).

## Outbound ports required

- `BankIntegrationPort`
- `ReceivableRepositoryPort`
- `SettlementRepositoryPort`

## REST endpoint

Not listed in the module spec's adapter table — this runs as a scheduled job via `BankIntegrationPort`, not a user-facing endpoint.

## Domain entities touched

- `Receivable`, `Settlement`

## Acceptance criteria

- [ ] Every matched, paid line in the CNAB file produces a `Settlement` with `method = AUTOMATIC_CNAB`.
- [ ] The matched `Receivable`'s `status` moves to `SETTLED` or `PARTIALLY_SETTLED`.
- [ ] Unmatched lines are reported, not silently dropped.
- [ ] Import runs daily without user interaction.

## Dependencies

- **Depends on:** UC-M8-03 (a boleto must exist for the bank to return on).
- **Blocks:** UC-M8-08, UC-M8-09 (aging/statement reflect these settlements).

## Notes

- Flag for the team: confirm whether an ops-facing manual-trigger/upload endpoint is needed for this job, since the source doc only describes the automatic daily import.

## Implementation notes

- **Trigger:** `BankReturnImportScheduler` (`gravita.finance.bank-return.import-cron`, default 06:00 daily) runs `ImportDailyBankReturnUseCase` for each bank in `gravita.finance.bank-return.banks` (empty by default), which fetches the bank's file via `BankIntegrationPort.fetchReturnFile` and delegates to `ImportBankReturnUseCase`. No REST endpoint; the manual-trigger/upload question above is still open for product (Atena).
- **Matching:** a return line's title identifier must be the `Receivable` id the bank echoes back. Lines whose identifier is not a receivable id, or matches no receivable, are reported in `BankReturnImportResult.unmatchedLines` with a reason.
- **Settlement status:** the receivable is `SETTLED` once its settlements' principal + discount cover its amount, otherwise `PARTIALLY_SETTLED`. `Settlement.timestamp` is the bank's payment date (UTC start of day).
- **Idempotency:** a line for a payment already recorded (same receivable, principal and payment date) or for a receivable that is not `OPEN`/`PARTIALLY_SETTLED` is reported as unmatched, never settled twice. Non-payment occurrences (registration, rejection) are counted in `skippedCount`.
- **Not yet implemented:** the per-bank CNAB 240/400 parsing behind `BankIntegrationPort.parseReturnFile`/`fetchReturnFile` — `BankIntegrationAdapter` still throws `BankIntegrationUnavailableException`, as it does for boletos and PIX.
- **Follow-ups:** none — `Settlement` now also links to a `Payable` (`payable_id`), used by [UC-M8-22](uc-22-confirm-batch-payment.md).
