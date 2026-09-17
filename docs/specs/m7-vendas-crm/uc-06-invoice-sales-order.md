# UC-M7-06 — Invoice Sales Order (`InvoiceSalesOrderUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.1 — Faturamento: generates NFe, NFCe or NFSe depending on item configuration (product × service).

## Description

Invoices an `APPROVED`/`IN_SEPARATION` order. For each line item, the fiscal document type is chosen by item nature — products go through NFe or NFCe, services go through NFSe — and issuance is delegated to `tax` via `IssueFiscalDocumentPort`. The resulting fiscal document(s) are linked back to the order through a `SalesInvoice`, the order moves to `INVOICED`, and an accounts-receivable title is generated in `finance`.

## Port signature

```java
public interface InvoiceSalesOrderUseCase {
    SalesInvoiceView execute(InvoiceSalesOrderCommand command);
}
```

`InvoiceSalesOrderCommand`: `orderId`. Returns the `SalesInvoice` (order reference, issued fiscal document reference(s), status).

## Outbound ports required

- `SalesOrderRepositoryPort`
- `IssueFiscalDocumentPort` (into `tax`)
- `GenerateAccountsReceivablePort` (into `finance`)

## REST endpoint

`POST /api/sales/orders/{id}/invoice`

## Domain entities touched

- `SalesOrder`
- `SalesInvoice`

## Acceptance criteria

- [ ] Only `APPROVED` or `IN_SEPARATION` orders can be invoiced.
- [ ] Product line items dispatch to NFe or NFCe issuance (M2/M3); service line items dispatch to NFSe issuance (M4).
- [ ] `SalesInvoice` links the order to every fiscal document actually issued.
- [ ] Order status transitions to `INVOICED` only after fiscal issuance succeeds.
- [ ] An accounts-receivable title is generated in `finance` matching the invoiced total.

## Dependencies

- **Depends on:** [UC-04 Approve Sales Order](uc-04-approve-sales-order.md).
- **Blocks:** [UC-07 Return Sales Order](uc-07-return-sales-order.md).

## Notes

- Dispatches to M2/M3/M4 issuance depending on item type (product → NFe/NFCe, service → NFSe); triggers M8 receivable generation. See [m2-fiscal-nfe.md](../m2-fiscal-nfe.md), [m4-fiscal-nfse.md](../m4-fiscal-nfse.md), [m8-financeiro.md](../m8-financeiro.md).
