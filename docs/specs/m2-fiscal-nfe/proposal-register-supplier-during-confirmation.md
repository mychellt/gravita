# Proposal — Register the supplier (and products) during NF-e receipt confirmation

**Status:** Proposal (not implemented) · **Module:** M2 — Fiscal: NFe, touches M1 — Cadastros base
**Related:** [UC-M2-10](uc-10-confirm-inbound-nfe-receipt.md), [UC-M2-15](uc-15-resolve-inbound-nfe-supplier-and-products.md), [UC-M1-09](../m1-cadastros-base/uc-09-register-supplier.md), [UC-M1-11](../m1-cadastros-base/uc-11-register-product.md), [UC-M1-16](../m1-cadastros-base/uc-16-manage-supplier-product-catalog-link.md)

## 1. Problem

The confirmation modal ("Conferir NF-e") only works when the NF-e's supplier already exists. When it doesn't, the user sees a warning that tells them to *"Cadastre o fornecedor"* — but the modal offers no way to do that, and the web app has **no supplier screen at all** (`purchasing` still runs on mock data). The user is stuck: the "Confirmar recebimento" button can never succeed, and nothing says how to get unstuck.

Items are in better shape: UC-M2-15 already lets the operator register a missing product or link an existing one from the modal. But that is only discoverable once the supplier exists, and the page never says it up front.

## 2. Goals (from the request)

| # | Goal | Today |
|---|---|---|
| 1 | If the supplier is not in the database, it can be registered during the confirmation. | **Missing.** Dead-end banner, no action. |
| 2 | If an NF-e product is not in the database, it can be registered during the confirmation. | **Exists** (`RegisterProductModalComponent`, `POST …/items/{i}/register-product`). Only reachable after the supplier is resolved. |
| 3 | The page must make both possibilities explicit to the user. | **Missing.** Only item-level hints once the supplier exists. |
| 4 | A product already in the database is used by the process. | **Exists**: a prior `SupplierProductCatalogLink` pre-fills the product (BR3); otherwise the operator links an existing one (BR5). Gap: no *suggestion* for a first-time supplier code (see §6). |

So the real work is goal 1 and goal 3; goals 2 and 4 are mostly wiring and wording.

## 3. Proposed behaviour

Open the confirmation modal and the user always sees, at the top, a **status checklist** with what is resolved and what they can do about it:

```
Conferir NF-e #777 — Fornecedor Alfa

  Antes de confirmar, a nota precisa de um fornecedor e de um produto para cada item.
  Você pode cadastrar o que estiver faltando aqui mesmo, sem sair desta tela.

  ✔ Fornecedor        Fornecedor Alfa (CNPJ 11.222.333/0001-81)
  ✖ Fornecedor        CNPJ 11.222.333/0001-81 não está cadastrado   [ Cadastrar fornecedor ]
  ◐ Itens             2 de 5 vinculados a um produto

  Item 3 — PARAFUSO M8  (cód. 1234)
     Este produto ainda não existe no cadastro.   [ Cadastrar produto novo ]  [ Vincular produto existente ]
```

Rules:

1. **Supplier unresolved →** a prominent banner with a **"Cadastrar fornecedor"** button replaces today's dead-end text. Items stay visible but their register/link actions are disabled with the reason "cadastre o fornecedor primeiro" (a link needs a `supplierId`).
2. **Supplier form** opens as a modal (same pattern as `RegisterProductModalComponent`), pre-filled from the NF-e: CNPJ (read-only — it is the key we match on) and name. The operator completes what the XML does not carry (§5). On save the conference reloads; `supplierResolved` flips to `true` and the item actions unlock. Nothing is persisted on the `InboundNfe` (UC-M2-15 BR8 holds).
3. **Items** keep the two existing actions per unresolved item — *Cadastrar produto novo* and *Vincular produto existente* — and show an explicit "Este produto ainda não existe no cadastro" line, so the choice is stated, not implied.
4. **Resolved items** show "Produto vinculado: …" as today and use that product for the stock entry — no re-picking (goal 4).
5. **Confirm button** stays disabled-with-reason (already implemented: the click now reports the first blocker) and the checklist counts update live.

## 4. UX details (goal 3 — explicit to the user)

- Intro text under the modal title (replace the current `impact-note`) saying *both* can be registered here.
- The checklist above; every red/amber row has its own action button next to it.
- Empty-state wording for the supplier banner: **"O CNPJ desta nota não está cadastrado como fornecedor. Cadastre-o agora para continuar — a conferência será mantida."**
- After a successful supplier/product registration, a toast ("Fornecedor cadastrado e vinculado a esta nota") and focus moves to the next pending row.
- Keep the quantities the operator typed across reloads (already implemented in `aplicar`).

## 5. Technical design

### 5.1 Backend — mostly reuse

| Piece | Status | Note |
|---|---|---|
| `POST /api/suppliers` (UC-M1-09) | exists | Requires `personType`, `document`, `name`, **at least one address** (street, neighborhood, city, state, zipCode). Contacts, bank account, PIX, lead time, default CFOP optional. |
| `GET /api/nfe/inbound/{id}/conference` | exists | Already returns `supplierResolved`/`supplierId`; re-read after registering. |
| CNPJ / CEP lookup for pre-fill | exists (`GET /api/lookup/cnpj/{cnpj}`, `GET /api/lookup/cep/{cep}` — `LookupRestController`, UC-M1-07) | Use the CNPJ lookup to pre-fill name/address and the CEP lookup to complete street/neighborhood/city/UF. Confirm in design what `PersonLookupResponse` returns for a CNPJ. |

**Recommended new endpoint (optional, thin):** `POST /api/nfe/inbound/{id}/register-supplier` taking the same body as `RegisterSupplierRequest` but with the document **forced** to the NF-e's `supplierDocument`. Reason: the generic `POST /api/suppliers` would let the operator register a *different* CNPJ than the one on the note, leaving the supplier unresolved and confusing them. The new endpoint guarantees the registered supplier is the one the NF-e matches. It delegates to `RegisterSupplierUseCase`; no new domain logic. If we prefer zero backend work, the front end can call `POST /api/suppliers` with the document locked in the form and rely on the re-read to confirm resolution.

Open decision: new endpoint (safer, small) vs. reuse `POST /api/suppliers` (no backend change).

### 5.2 Front end

1. `SupplierService` (new, `core/services`): `register(request)` → `POST /api/suppliers`, typed `RegisterSupplierRequest`/`SupplierResponse`.
2. `RegisterSupplierModalComponent` (new, `modules/nfe/inbound/`), modeled on `RegisterProductModalComponent`: CNPJ locked, name pre-filled, address block (CEP, street, number, complement, neighborhood, city, UF) required; optional contact. Emits `registered` → parent calls `carregar()`.
3. `ConfirmReceiptModalComponent`: add the status checklist, the "Cadastrar fornecedor" button, disabled-with-reason item actions while the supplier is unresolved, and the new intro/empty-state copy. Reuse the existing `impedimento` messages.
4. Specs (`inbound-flows.spec.ts`): supplier banner shows the action; register opens the form with CNPJ locked; success reloads and unlocks items; backend refusal keeps the form open with the message; item actions disabled before the supplier is resolved.

### 5.3 What does *not* change

`ConfirmInboundNfeReceiptUseCase` still rejects an unresolved supplier or item (UC-M2-10, UC-M2-15 AC2/AC3). This proposal only gives the user a way to satisfy those rules from inside the flow.

## 6. Open questions / risks

1. **Address is mandatory but the XML parser only reads CNPJ + name** (`ParsedSupplierNfe`). Options: (a) the operator types the address; (b) extend the parser to read `emit/enderEmit` (street, number, neighborhood, city, UF, CEP) so the form is nearly pre-filled — recommended, small, and removes most typing. Needs a parser change plus tests, and manual-entry NF-e (UC-M2-09) has no address, so (a) must still work.
2. **Permissions:** who may register suppliers from here? Reuse the existing supplier-registration permission; the modal should hide the button (and say why) for users without it.
3. **Goal 4 suggestion (optional):** for a first-time supplier code, suggest existing products by NCM/description in the picker. Fuzzy matching is explicitly *out of scope* in UC-M2-15; keep it out unless requested. Today the operator searches manually.
4. **Duplicate prevention:** registering a supplier whose CNPJ already exists must show a clear error (same treatment as the duplicate access key on import).
5. **Several suppliers per CNPJ across companies:** suppliers are company-scoped; registration must use the NF-e's `companyId`, as links already do (UC-M2-15 AC9).

## 7. Acceptance criteria

- [ ] **AC1** Opening the confirmation of an NF-e whose supplier is not registered shows a banner with a **"Cadastrar fornecedor"** action and an explanation that products can be registered in the same screen.
- [ ] **AC2** The supplier form is pre-filled with the NF-e's CNPJ (locked) and name, and requires at least one valid address.
- [ ] **AC3** Saving the supplier reloads the conference; `supplierResolved` becomes true and the item actions are enabled — without leaving the modal or losing typed quantities.
- [ ] **AC4** While the supplier is unresolved, item register/link actions are disabled and say why.
- [ ] **AC5** For every unresolved item the page states explicitly that the product is not in the registry and offers *Cadastrar produto novo* and *Vincular produto existente*.
- [ ] **AC6** An item that already has a link (or is linked during this flow) shows the linked product and that product is used for the stock entry; the operator is never asked to pick it again.
- [ ] **AC7** A checklist at the top of the modal shows supplier status and "N de M itens vinculados", updating live.
- [ ] **AC8** A refused registration (duplicate CNPJ, missing field) keeps the form open with the backend message.
- [ ] **AC9** A user without permission to register suppliers sees the action explained but disabled.
- [ ] **AC10** `ConfirmInboundNfeReceiptUseCase` behaviour and the imported `InboundNfe` snapshot are unchanged.

## 8. Suggested delivery

1. Spec update: add this as **UC-M2-16** and amend UC-M2-15 *Web* (this document is the draft).
2. Backend (small): optional `register-supplier` endpoint; parser reads `enderEmit` (if option 6.1b is accepted).
3. Front end: `SupplierService`, `RegisterSupplierModalComponent`, checklist + copy in `ConfirmReceiptModalComponent`, specs.
4. Manual test with an NF-e of an unregistered supplier with 1 known and 2 unknown products.

Estimated size: ~1–2 days front end, ≤½ day backend.
