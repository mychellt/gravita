# UC-M8-01 — Generate Receivable from Invoicing (`GenerateReceivableFromInvoicingUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.1 Contas a Receber — "Origem: faturamento (automático)": a receivable is created automatically whenever a sale or service is invoiced, with no manual entry.

## Description

Triggered internally when M7's `InvoiceSalesOrderUseCase` issues a fiscal document (NFe/NFCe), or when an M4 NFSe is authorized for a service sale — never called directly by a user. Given the invoice's customer, total amount and payment terms, it creates one `Receivable` per installment, `origin = INVOICING`, `status = OPEN`. Failure here must not roll back the already-authorized fiscal document; it should be retried and alerted (see M10 monitoring) rather than block issuance.

## Port signature

```java
public interface GenerateReceivableFromInvoicingUseCase {
    List<Receivable> execute(GenerateReceivableFromInvoicingCommand command);
}
```

`GenerateReceivableFromInvoicingCommand`: `customer` (ref), `originDocumentRef` (NFe/NFCe/NFSe id), `installments: [{dueDate, amount: Money}]`. Returns the created `Receivable` list.

## Outbound ports required

- `ReceivableRepositoryPort`

## REST endpoint

None — invoked internally via M7's `GenerateAccountsReceivablePort` and M4's NFSe-authorization flow, not exposed as a REST endpoint.

## Domain entities touched

- `Receivable`

## Acceptance criteria

- [ ] One `Receivable` is created per installment in the invoice's payment terms.
- [ ] `origin` is set to `INVOICING` and `status` to `OPEN`.
- [ ] Each receivable references the originating fiscal document.
- [ ] Creation is idempotent per `originDocumentRef` — re-invoicing the same document doesn't duplicate titles.

## Dependencies

- **Depends on:** M7 `InvoiceSalesOrderUseCase`, M4 NFSe authorization.
- **Blocks:** UC-M8-03, UC-M8-04, UC-M8-06, UC-M8-07, UC-M8-08, UC-M8-09 (all read or act on `Receivable`).
