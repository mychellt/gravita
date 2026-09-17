# UC-M7-02 — Send Quote (`SendQuoteUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.1 — Orçamento (quote): printed as PDF or sent via WhatsApp.

## Description

Delivers an existing quote to the customer, either as a PDF document or via WhatsApp. On successful delivery the quote transitions to `SENT`. Delivery of an already-expired quote is rejected — the salesperson must create a new one.

## Port signature

```java
public interface SendQuoteUseCase {
    void execute(SendQuoteCommand command);
}
```

`SendQuoteCommand`: `quoteId`, `channel: {PDF, WHATSAPP}`.

## Outbound ports required

- `QuoteRepositoryPort`
- `SendQuoteByWhatsAppPort`

## REST endpoint

`POST /api/sales/quotes/{id}/send`

## Domain entities touched

- `Quote`

## Acceptance criteria

- [ ] Quote status transitions to `SENT` on successful delivery.
- [ ] Sending an expired (`validUntil` passed) quote is rejected.
- [ ] `WHATSAPP` channel uses the customer's registered WhatsApp contact from `masterdata`.

## Dependencies

- **Depends on:** [UC-01 Create Quote](uc-01-create-quote.md).
- **Blocks:** —

## Notes

- The module spec doesn't name a dedicated PDF-rendering port for quotes (unlike M9's `RenderPdfPort` for reports); PDF generation is assumed to reuse a shared document-rendering capability, not modeled as a separate outbound port here.
