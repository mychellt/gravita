# M1 — Cadastros Base: Implementation Tickets

Index of the per-use-case tickets for [M1 — Cadastros Base](../m1-cadastros-base.md). All are Phase 1 — Fundação (doc §14); build roughly in the numbered order below, since later tickets generally depend on earlier ones (see each ticket's **Dependencies** section for the exact edges).

| # | Ticket | Phase | Responsibility |
|---|---|---|---|
| 01 | [Register Company](uc-01-register-company.md) | 1 — Fundação | Create a company/branch, its fiscal data and initial document series. |
| 02 | [Upload Digital Certificate](uc-02-upload-digital-certificate.md) | 1 — Fundação | Attach/replace the A1 certificate for a company. |
| 03 | [Switch SEFAZ Environment](uc-03-switch-sefaz-environment.md) | 1 — Fundação | Toggle Production/Homologation for a company without a restart. |
| 04 | [Configure Document Series](uc-04-configure-document-series.md) | 1 — Fundação | Set series/next number per document type. |
| 05 | [Register Customer](uc-05-register-customer.md) | 1 — Fundação | Create a customer, including CPF/CNPJ validation. |
| 06 | [Update Customer](uc-06-update-customer.md) | 1 — Fundação | Edit an existing customer. |
| 07 | [Lookup Person by Document](uc-07-lookup-person-by-document.md) | 1 — Fundação | Auto-fill name/address via Receita Federal and ViaCEP; shared by customer and supplier registration. |
| 08 | [Set Customer Credit Status](uc-08-set-customer-credit-status.md) | 1 — Fundação | Update limit/status; invoked by `finance` when delinquency changes. |
| 09 | [Register Supplier](uc-09-register-supplier.md) | 1 — Fundação | Create a supplier. |
| 10 | [Update Supplier](uc-10-update-supplier.md) | 1 — Fundação | Edit an existing supplier. |
| 11 | [Register Product](uc-11-register-product.md) | 1 — Fundação | Create a product, service or kit, including its tax profile. |
| 12 | [Update Product](uc-12-update-product.md) | 1 — Fundação | Edit an existing product, service or kit. |
| 13 | [Manage Price Table](uc-13-manage-price-table.md) | 1 — Fundação | Create/edit price tables and entries, including validity transitions. |
| 14 | [Manage Auxiliary Table](uc-14-manage-auxiliary-table.md) | 1 — Fundação | CRUD for payment terms/methods, cost centers, chart of accounts, IBGE municipalities, interstate ICMS rates. |
| 15 | [Allocate Document Number](uc-15-allocate-document-number.md) | 1 — Fundação | Atomically reserve the next number for a document series (called by `tax`, not user-facing). |
