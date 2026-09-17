# UC-M8-03 — Generate Boleto (`GenerateBoletoUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.1 Contas a Receber — "Boleto bancário: geração e envio por e-mail; integração com bancos principais via API (Itaú, BB, Bradesco, Sicoob, Sicredi)."

## Description

For an existing `Receivable`, generates a `Boleto` via the bank integration and e-mails it to the customer. Precondition: the receivable exists and is `OPEN`. Postcondition: a `Boleto` is linked to the receivable, carrying its barcode line, and delivery is triggered automatically.

## Port signature

```java
public interface GenerateBoletoUseCase {
    Boleto execute(GenerateBoletoCommand command);
}
```

`GenerateBoletoCommand`: `receivableId`, `bankIntegration` (which configured bank to use). Returns the created `Boleto`.

## Outbound ports required

- `ReceivableRepositoryPort`
- `BankIntegrationPort`

## REST endpoint

`POST /api/finance/receivables/{id}/boleto`

## Domain entities touched

- `Receivable`, `Boleto`

## Acceptance criteria

- [ ] A `Boleto` is generated with a valid barcode line from the chosen bank integration.
- [ ] The boleto is e-mailed to the customer automatically on generation.
- [ ] Generation fails cleanly if the receivable isn't `OPEN`.

## Dependencies

- **Depends on:** UC-M8-01 or UC-M8-02 (a `Receivable` must exist).
- **Blocks:** —
