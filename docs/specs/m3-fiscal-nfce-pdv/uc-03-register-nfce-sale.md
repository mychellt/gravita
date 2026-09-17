# UC-M3-03 — Register NFCe Sale (`RegisterNfceSaleUseCase`)

**Module:** M3 — Fiscal: NFCe + PDV ([module spec](../m3-fiscal-nfce-pdv.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§4.1: "Add to cart: numeric keypad for quantity; product added with no extra confirmation step." "Discount: per item or total; the max-discount limit is enforced automatically." "Payment methods: multiple within the same sale... change calculated automatically." "CPF na nota: optional field; entered by typing, QR code, or the registered customer's CPF."

## Description

The cashier builds the cart from [UC-02](uc-02-search-product-for-sale.md)'s search results, applies per-item or total discounts (capped by the linked price table's max-discount rule), optionally records the CPF na nota, and takes one or more payment methods. This produces a `DRAFT` `NfceSale`, ready for issuance in [UC-04](uc-04-issue-nfce.md).

## Port signature

```java
public interface RegisterNfceSaleUseCase {
    NfceSaleId execute(RegisterNfceSaleCommand command);
}
```

`RegisterNfceSaleCommand`: `sessionId`, `items: [{productId, quantity, unitPrice, itemDiscount}]`, `totalDiscount` (optional), `payments: [{method, amount}]`, `customerCpf` (optional). Returns the id of the created `NfceSale` (status `DRAFT`).

## Outbound ports required

- `NfceRepositoryPort`

## REST endpoint

`POST /api/pdv/sales` (this use case and [UC-04 — Issue NFCe](uc-04-issue-nfce.md) are orchestrated behind the same endpoint per the module spec's adapter table: register, then issue).

## Domain entities touched

- `NfceSale` (created), `SaleItem`, `Payment` (child value objects)

## Acceptance criteria

- [ ] Quantity is entered via the numeric keypad; the product is added to the cart with no extra confirmation step.
- [ ] Discount, per item or total, never exceeds the linked price table's `maxDiscountPercent`; behavior on breach follows the table's `maxDiscountBehavior` (`BLOCK` or `ALERT`).
- [ ] Multiple payment methods can be combined in the same sale (e.g. cash + card).
- [ ] `changeGiven` is derived (`sum(payments) − saleTotal`), never entered directly.
- [ ] Total payments must cover the sale total, or the sale is rejected (domain invariant).
- [ ] `customerCpf` is optional and accepts typed entry, QR code, or a registered customer's CPF.

## Dependencies

- **Depends on:** [01 — Open Session](uc-01-open-pos-session.md), [02 — Search Product](uc-02-search-product-for-sale.md).
- **Blocks:** [04 — Issue NFCe](uc-04-issue-nfce.md) — issuance operates on the sale this use case produces.
