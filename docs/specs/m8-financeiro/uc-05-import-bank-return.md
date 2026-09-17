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
