# M1 — Cadastros Base (Master Data)

Source: ERP MVP doc §2. Context package: `br.gravita.masterdata`.

## Purpose

Base registrations are the foundation of every other module. Errors or missing fields here propagate into fiscal, financial and operational inconsistencies elsewhere, so every registration screen validates in real time and shows a completeness indicator.

## Functional requirements

### 2.1 Empresa / Filiais (Company / Branches)

- Fiscal data: CNPJ, IE, IM, main CNAE, tax regime (Simples Nacional / Lucro Presumido / Lucro Real), Simples opt-in flag.
- Digital certificate: upload and manage an A1 (`.pfx`) certificate per company; A3 (token) support is future work.
- SEFAZ environment: Production / Homologation, set per company, switchable without restarting the system.
- Document series: series and next number configured per company for NFe, NFCe and NFSe.
- Contact data: full address, issuing e-mail, phone, logo for the DANFE.
- Multi-company: the data model is prepared for it; activation is a config flag, with no migration required.

### 2.2 Clientes (Customers)

- Type: PF (CPF) or PJ (CNPJ), with real-time check-digit validation.
- Auto lookup: company name and address filled automatically via Receita Federal (by CNPJ) and ViaCEP (by CEP).
- Fiscal data: IE indicator (taxpayer / exempt / non-taxpayer), final-consumer flag.
- Multiple addresses: independent billing and delivery addresses; configurable default.
- Credit and risk: credit limit, current balance, status (regular / blocked / delinquent).
- Price table: linked to one or more price tables with configurable priority.
- Contact data: e-mail, WhatsApp, phone; multiple contacts per customer.
- History: purchases, payments, returns and CRM interactions shown on a single timeline.

### 2.3 Fornecedores (Suppliers)

- Base registration: same PF/PJ structure as customers, reusing the same form.
- Bank data: checking account, agency, bank; PIX key (CPF, CNPJ, e-mail or random key).
- Operation data: average delivery lead time in days, default purchase CFOP.
- History: purchase orders, returns, quality occurrences.

### 2.4 Produtos e Serviços (Products & Services)

- Identification: internal code (auto or manual), multiple barcodes (EAN-13, DUN-14).
- Type: simple product, product with variant grid (color/size), kit/composition, service.
- Fiscal data: NCM, CEST, origin (0–8), default CFOP per operation, CST/CSOSN per destination state.
- Taxes: ICMS, IPI, PIS, COFINS, ICMS-ST, FCP — per tax profile and state.
- Prices: average cost, base sale price, calculated margin; multiple price tables.
- Stock: minimum, maximum, reorder point, purchase vs. sale unit and conversion factor.
- Traceability: lot control (with expiry date), serial number — activatable per product.
- Classification: group, subgroup, brand, section — used in reports and the ABC curve.
- Images: up to 5 photos per product; shown in the PDV and in quotes.
- Status: active, inactive, out-of-stock (hidden from sale, history kept).

### 2.5 Tabelas de Preço (Price Tables)

- Unlimited tables; each customer or sales channel can have its own.
- Price formation: fixed price, percentage over cost, or percentage over base price.
- Validity: start and end date, with automatic transition at date rollover.
- Max discount: configurable percentage limit per table; block or alert the salesperson.

### 2.6 Tabelas Auxiliares (Auxiliary Tables)

- Payment terms: cash, 30/60/90, installments — free number of installments and intervals.
- Payment methods: cash, debit/credit card, PIX, boleto, store credit, voucher — configurable.
- Cost centers: hierarchical tree, used in expenses, sales and the DRE report.
- Chart of accounts: simplified structure for managerial DRE and accounting export.
- IBGE municipalities: full table, pre-loaded; required for ISS/NFSe calculation.
- Interstate ICMS: rate table by origin × destination state; updated manually via import.

## Domain model

- **Company** (aggregate root) — `cnpj: Document`, `ie`, `im`, `cnae`, `taxRegime: TaxRegime {SIMPLES_NACIONAL, LUCRO_PRESUMIDO, LUCRO_REAL}`, `simplesOptante: boolean`, `sefazEnvironment: SefazEnvironment {PRODUCTION, HOMOLOGATION}`, `address`, `issuingEmail`, `phone`, `logoUrl`. Owns `Branch` entities and `DocumentSeries` (one per document type: NFe, NFCe, NFSe).
- **DigitalCertificate** — value object/entity attached to a `Company`: encrypted `.pfx` payload, expiry date, type (`A1`; `A3` reserved). Invariant: a company cannot issue any fiscal document while its active certificate is expired or absent.
- **DocumentSeries** — `documentType`, `series`, `nextNumber` per `Company`. Invariant: number allocation must be free of duplicates under concurrent issuance (see [Cross-module dependencies](#cross-module-dependencies) and doc §13, "Numeração de séries").
- **Person** (shared shape for Customer and Supplier) — `type: PersonType {PF, PJ}`, `document: Document`, name/company name, addresses (billing/delivery, each with a default flag), contacts.
- **Customer** (aggregate root, embeds `Person`) — `ieIndicator: {TAXPAYER, EXEMPT, NON_TAXPAYER}`, `finalConsumer: boolean`, `creditLimit: Money`, `currentBalance: Money`, `status: {REGULAR, BLOCKED, DELINQUENT}`, linked `PriceTable`s with priority order. Invariant: `status` derives from `currentBalance` vs. `creditLimit` plus overdue titles (fed by `finance`, read-only here).
- **Supplier** (aggregate root, embeds `Person`) — `bankAccount`, `pixKey`, `averageLeadTimeDays`, `defaultPurchaseCfop`.
- **Product** (aggregate root) — `internalCode`, `barcodes: [EAN13|DUN14]`, `type: {SIMPLE, VARIANT, KIT, SERVICE}`, `ncm`, `cest`, `origin (0–8)`, `defaultCfopByOperation`, `cstCsosnByState`, `taxProfile` (ICMS/IPI/PIS/COFINS/ICMS-ST/FCP parameters), `averageCost: Money`, `basePrice: Money`, `stock{min,max,reorderPoint}`, `purchaseUnit`, `saleUnit`, `conversionFactor`, `lotControl: boolean`, `serialControl: boolean`, `classification{group,subgroup,brand,section}`, `images: [URL] (max 5)`, `status: {ACTIVE, INACTIVE, OUT_OF_STOCK}`. A `Kit` variant composes other `Product`s; a `Service` variant carries no stock fields.
- **PriceTable** — `formation: {FIXED, PERCENT_OVER_COST, PERCENT_OVER_BASE}`, `validFrom`, `validTo`, `maxDiscountPercent`, `maxDiscountBehavior: {BLOCK, ALERT}`, list of `PriceTableEntry` (per product or product class).
- **PaymentTerm**, **PaymentMethod**, **CostCenter** (tree), **ChartOfAccounts** (tree), **IbgeMunicipality**, **InterstateIcmsRate** — auxiliary reference entities, all read by other contexts but owned here.

## Use cases (`application.port.in`)

Each use case below has a standalone implementation ticket under [`m1-cadastros-base/`](m1-cadastros-base/README.md), ordered as the build sequence.

| Use case | Responsibility |
|---|---|
| [`RegisterCompanyUseCase`](m1-cadastros-base/uc-01-register-company.md) | Create a company/branch, its fiscal data and initial document series. |
| [`UploadDigitalCertificateUseCase`](m1-cadastros-base/uc-02-upload-digital-certificate.md) | Attach/replace the A1 certificate for a company. |
| [`SwitchSefazEnvironmentUseCase`](m1-cadastros-base/uc-03-switch-sefaz-environment.md) | Toggle Production/Homologation for a company without a restart. |
| [`ConfigureDocumentSeriesUseCase`](m1-cadastros-base/uc-04-configure-document-series.md) | Set series/next number per document type. |
| [`RegisterCustomerUseCase`](m1-cadastros-base/uc-05-register-customer.md) | Create a customer, including CPF/CNPJ validation. |
| [`UpdateCustomerUseCase`](m1-cadastros-base/uc-06-update-customer.md) | Edit an existing customer. |
| [`LookupPersonByDocumentUseCase`](m1-cadastros-base/uc-07-lookup-person-by-document.md) | Auto-fill name/address via Receita Federal (CNPJ) and ViaCEP; used by both customer and supplier registration. |
| [`SetCustomerCreditStatusUseCase`](m1-cadastros-base/uc-08-set-customer-credit-status.md) | Update limit/status; invoked by `finance` when delinquency changes. |
| [`RegisterSupplierUseCase`](m1-cadastros-base/uc-09-register-supplier.md) | Create a supplier. |
| [`UpdateSupplierUseCase`](m1-cadastros-base/uc-10-update-supplier.md) | Edit an existing supplier. |
| [`RegisterProductUseCase`](m1-cadastros-base/uc-11-register-product.md) | Create a product, service or kit, including its tax profile. |
| [`UpdateProductUseCase`](m1-cadastros-base/uc-12-update-product.md) | Edit an existing product, service or kit. |
| [`ManagePriceTableUseCase`](m1-cadastros-base/uc-13-manage-price-table.md) | Create/edit price tables and entries, including validity transitions. |
| [`ManageAuxiliaryTableUseCase`](m1-cadastros-base/uc-14-manage-auxiliary-table.md) | CRUD for payment terms/methods, cost centers, chart of accounts, IBGE municipalities, interstate ICMS rates. |
| [`AllocateDocumentNumberUseCase`](m1-cadastros-base/uc-15-allocate-document-number.md) | Atomically reserve the next number for a document series (called by `tax`, not user-facing). |

## Outbound ports (`application.port.out`)

| Port | Purpose |
|---|---|
| `CompanyRepositoryPort`, `CustomerRepositoryPort`, `SupplierRepositoryPort`, `ProductRepositoryPort`, `PriceTableRepositoryPort` | Persistence for each aggregate. |
| `DocumentSeriesRepositoryPort` | Persistence with optimistic-lock support for numbering (doc §13). |
| `CnpjLookupPort` | Receita Federal CNPJ consultation. |
| `CepLookupPort` | ViaCEP address consultation. |
| `CertificateStoragePort` | Encrypted at-rest storage for the A1 certificate (doc §11.4). |

## Adapters

### Inbound (`adapter.in.web`)

| Method & path | Use case |
|---|---|
| `POST /api/companies` / `PATCH /api/companies/{id}` | `RegisterCompanyUseCase` / update |
| `POST /api/companies/{id}/certificate` | `UploadDigitalCertificateUseCase` |
| `PATCH /api/companies/{id}/sefaz-environment` | `SwitchSefazEnvironmentUseCase` |
| `PUT /api/companies/{id}/document-series/{type}` | `ConfigureDocumentSeriesUseCase` |
| `POST /api/customers` / `PATCH /api/customers/{id}` | `RegisterCustomerUseCase` / update |
| `GET /api/lookup/cnpj/{cnpj}`, `GET /api/lookup/cep/{cep}` | `LookupPersonByDocumentUseCase` |
| `POST /api/suppliers` / `PATCH /api/suppliers/{id}` | `RegisterSupplierUseCase` / update |
| `POST /api/products` / `PATCH /api/products/{id}` | `RegisterProductUseCase` / update |
| `POST /api/price-tables` / `PATCH /api/price-tables/{id}` | `ManagePriceTableUseCase` |
| `GET/POST/PATCH /api/auxiliary/{payment-terms|payment-methods|cost-centers|chart-of-accounts}` | `ManageAuxiliaryTableUseCase` |

### Outbound (`adapter.out.persistence` / integrations)

JPA entities mirroring each aggregate (`CompanyJpaEntity`, `BranchJpaEntity`, `CustomerJpaEntity`, `SupplierJpaEntity`, `ProductJpaEntity`, `PriceTableJpaEntity`, …) plus two external adapters: a Receita Federal client implementing `CnpjLookupPort`, and a ViaCEP client implementing `CepLookupPort`.

## Cross-module dependencies

- **Provides to `tax`**: company fiscal data, digital certificate, document series/numbering, product tax profile, customer/supplier fiscal data.
- **Provides to `sales`**: customer, product, price table.
- **Provides to `purchasing`**: supplier, product.
- **Provides to `inventory`**: product stock parameters (min/max/reorder point, lot/serial control flags).
- **Provides to `finance`**: customer/supplier bank and credit data, cost centers, chart of accounts.
- **Consumes from `finance`**: customer delinquency status, to keep `Customer.status` current (`SetCustomerCreditStatusUseCase`).

## Notes

- `Money` and `Percentage` are used here as shared value objects (see [00-overview.md](00-overview.md#technical-mapping-conventions)); they are proposed, not yet in `br.gravita.shared`.
- The PDF doesn't specify how `DocumentSeries` numbering concurrency is enforced at the M1 level; doc §13 assigns that to a centralized, optimistically-locked service, referenced here as `AllocateDocumentNumberUseCase`.
