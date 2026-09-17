# UC-M1-07 — Lookup Person by Document (`LookupPersonByDocumentUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.2 Clientes / §2.3 Fornecedores: auto lookup — company name and address filled automatically via Receita Federal (by CNPJ) and ViaCEP (by CEP); reused by both customer and supplier registration forms.

## Description

A user typing a CNPJ or CEP into the customer or supplier registration form triggers a lookup that returns the company name/address to pre-fill the form. This is a pure query, not a persistence operation — nothing is written to the domain by this use case itself. Precondition: none. Postcondition: none (read-only); the caller (UC-05 or UC-09) decides whether to use the result.

## Port signature

```java
public interface LookupPersonByDocumentUseCase {
    PersonLookupResult execute(LookupPersonByDocumentQuery query);
}
```

`LookupPersonByDocumentQuery`: `document: Document` (CNPJ) or `cep: String`. Returns `PersonLookupResult{name, address}` (partial — CEP-only queries return address without a name).

## Outbound ports required

- `CnpjLookupPort` (Receita Federal)
- `CepLookupPort` (ViaCEP)

## REST endpoint

`GET /api/lookup/cnpj/{cnpj}`, `GET /api/lookup/cep/{cep}`

## Domain entities touched

- None persisted — `PersonLookupResult` is a transient DTO, not a domain entity.

## Acceptance criteria

- [ ] Responds fast enough to satisfy the global 300ms search-debounce UX rule (doc §12.1) — i.e., it must not block the registration form.
- [ ] A CNPJ lookup returns company name and address in one call.
- [ ] A CEP lookup returns address only.
- [ ] An external-service failure (Receita Federal or ViaCEP down) degrades to manual entry — it never blocks form submission.
- [ ] The same use case is called from both the customer (UC-05) and supplier (UC-09) registration flows, with no duplicated integration code.

## Dependencies

- **Depends on:** None (pure integration query, no other M1 aggregate required).
- **Blocks:** Nothing formally — UC-05 and UC-09 can proceed without it — but it's the intended pre-fill path for both.
