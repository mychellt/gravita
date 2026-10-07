# The Life of a Sale — Main ERP Operations as a Story

A narrative walk-through of the operations that every market ERP covers (TOTVS Protheus, SAP Business One, Odoo, Omie, Bling and similar), told through one retailer. Each chapter names the operation the way ERPs usually call it, then maps it onto the Gravita modules M1–M10 in [`specs/00-overview.md`](specs/00-overview.md).

This is a functional narrative, not a spec. Where it goes beyond a module spec, the spec wins.

## The cast

- **Marina** — owner of *Casa Verde*, a home-and-garden retailer with one store, a small online channel and an installation service. Tax regime: Simples Nacional.
- **Rafael** — buyer / stock manager.
- **Júlia** — cashier and salesperson.
- **Paulo** — finance assistant.
- **Dona Lúcia** — accountant (external).

## The loop every ERP implements

```
 Master data ──► Purchase ──► Stock ──► Sale ──► Invoice ──► Receivable ──► Cash
     (M1)        (M6)        (M5)     (M3/M7)   (M2/M3/M4)     (M8)         (M8)
                                                      └────────────────────► Reports / SPED (M9, M2)
                 Users, permissions, audit, backup, alerts across everything (M10)
```

Market ERPs group this as **Procure-to-Pay** (chapters 2–3, 7), **Order-to-Cash** (chapters 4–6, 7) and **Record-to-Report** (chapters 8–9). Master data (chapter 1) and system administration (chapter 10) sit underneath all three.

---

## Chapter 1 — Setting the stage: master data (M1)

Before Casa Verde sells anything, Marina spends one afternoon on the data every other operation reads from.

- She registers the **company**: CNPJ, tax regime, address, and the **A1 digital certificate** that will sign every fiscal document. The SEFAZ environment starts in *homologação* (test); she switches to *produção* only when she is ready.
- She configures **document series** (NF-e series 1, NFC-e series 1). Numbers are allocated by the system, never typed, and never reused.
- She registers **products** with SKU, EAN, NCM, unit, cost and the tax fields (CFOP, CSOSN, origin). She builds a **price table** for retail and another for contractors.
- She registers **suppliers** and **customers**. Typing a CNPJ or CPF fills in the name and address from a public lookup; a customer can be flagged *blocked for credit*.

> Every variable business rule — CFOP, CSOSN, rates — lives in this data, not in code. That is why the same system serves a Simples Nacional shop and a Lucro Real distributor.

**Stories**
- As an owner, I want to register my company and certificate once so that every document is signed correctly afterwards.
- As a buyer, I want a product registered once with its tax data so that no one retypes it on a purchase, a sale or an invoice.
- As a salesperson, I want to type a CPF/CNPJ and get the customer filled in so that registration takes seconds.

## Chapter 2 — Rafael notices the shelf is empty: purchase request and quotation (M5 → M6)

Monday morning, Rafael's dashboard shows 14 items below minimum stock. The system has already drafted a **purchase request** for them (a *reorder suggestion*). A request can also come from a person, or from a customer order that stock cannot cover.

He sends the request to three suppliers as a **quotation**. Replies are registered as they come in and shown **side by side**: unit price, delivery deadline, total. Rafael picks the winner per item rather than per supplier.

**Stories**
- As a buyer, I want low-stock items turned into a purchase request automatically so that I never discover a stockout at the counter.
- As a buyer, I want to compare supplier quotes side by side so that I choose on price *and* deadline.

## Chapter 3 — Approval, order, and the truck at the door (M6 → M5)

The winning quotes become a **purchase order**. It exceeds Rafael's limit, so the **approval workflow** (*alçada* by value) routes it to Marina, who approves from her phone. The order is now *Open*.

Thursday the truck arrives with part of the order. Rafael opens the order and the system **imports the supplier's NF-e XML** — no retyping. He does the **physical conference**: ordered vs. arrived vs. on the invoice. One carton is short, so the receipt is **partial**. The order becomes *Partially Received*, and the system:

1. **Moves stock in**, updating the average cost.
2. **Generates the payable** from the invoice's installment terms.
3. Keeps the remainder of the order open.

A week later, 6 defective pots go back. A **purchase return** is registered and the **return NF-e** is issued in the same flow; stock and the payable adjust accordingly.

**Stories**
- As a manager, I want approval limits by value so that big purchases never go out without the right person.
- As a receiver, I want the supplier XML imported so that I only confirm quantities.
- As finance, I want the payable created at receiving so that nothing is paid or forgotten by hand.

## Chapter 4 — The shelf, the warehouse and the count (M5)

Between purchases and sales, stock is where the truth about the business lives.

- Every movement — purchase, sale, return, transfer, adjustment — is a **stock movement** with a document, a user and a timestamp. Quantities are derived from movements, never edited directly.
- Stock is tracked per **warehouse**, with **transfers** showing in-transit stock until the destination confirms; products can carry **lot/expiry** or **serial numbers**, and an expired lot can never be sold.
- Casa Verde runs a partial **physical count** (by product group) on Saturday mornings: Rafael enters the counted quantities, and once the count is **approved** the divergences become **adjustments**. Manual adjustments always require a justification.
- **Reorder points** feed back into chapter 2, closing the loop.

**Stories**
- As a stock manager, I want every quantity explained by a movement so that I can audit any difference.
- As a stock manager, I want counts and adjustments recorded with a reason so that losses are visible rather than silent.

## Chapter 5 — Júlia at the counter: PDV and NFC-e (M3)

Saturday, 10:12. A customer buys a watering can and two bags of soil. Júlia scans the barcodes in the **PDV** (*ponto de venda*). Prices come from the retail price table; she applies a 5% discount; the maximum-discount limit is enforced automatically, and a sale cancelled later needs a supervisor password.

She chooses **PIX**. The system:

1. Calculates tax with the central **tax engine**.
2. Issues and authorises the **NFC-e** with SEFAZ.
3. Prints the receipt and QR code.
4. Decrements stock and records the payment.

At 14:30 the internet drops. The PDV keeps selling in **offline contingency**; NFC-e documents are queued and transmitted automatically when the link returns. The customer never waits and Marina never fills a form.

During the day, a **sangria** (cash withdrawal) or **suprimento** (cash deposit) is recorded with a mandatory justification.

**Stories**
- As a cashier, I want to finish a sale in a few clicks so that the queue keeps moving.
- As an owner, I want sales to continue when the internet fails so that an outage never costs revenue.
- As an owner, I want the maximum discount enforced automatically so that the cashier can't give the store away.

## Chapter 6 — The contractor's big order: quote → order → NF-e, and services → NFS-e (M7, M2, M4)

On Tuesday a landscaper asks for 40 plants, drip irrigation, and installation.

1. Júlia creates a **quote** (*orçamento*) with the contractor price table and sends it as a PDF or by WhatsApp. It has a validity date; a **follow-up rule** ("no contact in X days") notifies her if the customer goes quiet.
2. He accepts. The quote converts to a **sales order** — nothing is retyped. The system checks **credit** (limit and overdue titles) and **stock availability**; the missing plants become a purchase request (chapter 2).
3. When goods are ready, the order is **invoiced**: an **NF-e** is issued, signed, transmitted and authorised; the **DANFE** is sent to the customer. If SEFAZ is down, the **retransmission queue** retries automatically.
4. The installation service is a different tax event: an **NFS-e** is issued through the municipality's standard (ABRASF, NFS-e Nacional, etc.), with **ISS withholding** where applicable.
5. If an item comes back, a **return** is registered, a **return NF-e** is issued and the receivable is adjusted.

A mistake is fixed by the legal route: **cancel** within the deadline, or issue a **correction letter (CC-e)**. A fiscal document is never deleted.

**Stories**
- As a salesperson, I want to turn a quote into an order and then an invoice without retyping so that errors don't creep in.
- As a finance lead, I want credit checked at order time so that we don't ship to a customer who is overdue.
- As an owner, I want failed fiscal transmissions retried automatically so that nothing stays stuck unnoticed.

## Chapter 7 — Money in, money out (M8)

Invoicing created **receivables**; receiving goods created **payables**. Paulo lives here.

**Accounts receivable**
- The contractor's invoice produced three installments. Paulo generates a **boleto** and a **PIX charge** for each.
- The bank's **return file (CNAB)** arrives; the system matches payments to titles and **settles them automatically**. Late titles show in the **aging list**; for a customer in trouble he **renegotiates** the title with a new schedule, keeping history.
- Customers can be given a **statement** of their open and paid items.

**Accounts payable**
- Supplier payables appear from receiving, or are entered manually (rent, energy). A payable above a threshold needs **approval**, can carry the **attached document**, and can be **split by cost centre**.
- On Friday Paulo selects everything due and does a **batch payment**, or pays by **PIX**; the bank confirms and the titles are settled.

**Treasury**
- The **cash-flow** view projects the next 30–90 days from open receivables and payables.
- Paulo **reconciles** the bank statement against system movements; unmatched lines are investigated, not ignored.
- Internal cash movements are recorded, and the **daily cash is closed**.

**Stories**
- As finance, I want bank returns to settle titles automatically so that no one ticks boxes by hand.
- As an owner, I want a cash-flow forecast so that I know if next month's payroll is covered.
- As finance, I want statement lines reconciled to titles so that the books match the bank.

## Chapter 8 — Marina asks "how are we doing?": reporting and BI (M9)

Marina opens the dashboard Monday morning, before Rafael's coffee is ready: sales by day, margin by category, top products, stock turnover, overdue receivables, cash position. Every number links down to the documents behind it. Standard reports cover sales by seller, purchases by supplier, stock valuation and DRE-style summaries; each exports to spreadsheet or PDF.

Casa Verde does not run a data warehouse. Advanced OLAP is deliberately outside the MVP.

**Stories**
- As an owner, I want a single dashboard of the numbers that matter so that I don't ask three people.
- As a manager, I want to drill from a number to the documents behind it so that I can trust it.

## Chapter 9 — Dona Lúcia closes the month: fiscal books and SPED (M2, M9)

At month end Dona Lúcia needs, without chasing anyone:

- All issued and received XMLs.
- **Fiscal books** (livros de entrada/saída) and the **SPED** files.
- An **accounting export** into her own system.

The ERP produces them from the same documents that already ran the operation. Because fiscal documents can't be deleted and every action is audited, the files are consistent with what actually happened.

**Stories**
- As an accountant, I want fiscal books and SPED generated from operational data so that I never reconcile two sources.

## Chapter 10 — The part nobody sees: system administration (M10)

Throughout, the system quietly does what makes the rest trustworthy:

- **Users, roles and permissions**: Júlia can sell and discount up to a limit; Paulo can pay but not approve; Marina can do everything.
- **Audit trail**: for every change — who, what, when, previous value, new value.
- **Integration credentials** for SEFAZ, banks and the NFS-e provider, stored securely.
- **Monitoring**: automatic alerts for transmission failures, delayed jobs and critical errors, so a stuck fiscal queue is noticed by the system before it is noticed by a customer.
- **Backup and restore**, tested.

**Stories**
- As an owner, I want to control who can do what so that risky actions have the right gate.
- As an owner, I want to know who changed a price and when so that I can answer any dispute.

---

## Summary: operations by module

| Operation family (market ERP term) | Chapters | Gravita module |
|---|---|---|
| Master data / cadastros | 1 | M1 |
| Procure-to-Pay: requisition, RFQ, PO, goods receipt, return | 2–3 | M6 (+ M5, M2 import) |
| Inventory: movements, locations, counts, reorder | 4 | M5 |
| Point of sale, retail fiscal receipt | 5 | M3 |
| Order-to-Cash: quote, order, invoice, return | 6 | M7 + M2 |
| Service billing | 6 | M4 |
| AR / AP / treasury / reconciliation | 7 | M8 |
| Reporting & BI | 8 | M9 |
| Fiscal books, SPED, accounting export | 9 | M2 / M9 |
| Security, audit, monitoring, backup | 10 | M10 |

## What the story deliberately leaves out

Following the MVP's non-goals ([`specs/11-ux-e-decisoes-tecnicas.md`](specs/11-ux-e-decisoes-tecnicas.md)): payroll/HR, full bookkeeping, a proprietary e-commerce storefront, advanced BI/OLAP, a native mobile app and SAP integration. Many large market ERPs bundle these; here they are separate products or later phases.
