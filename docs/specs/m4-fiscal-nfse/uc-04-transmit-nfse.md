# UC-M4-04 — Transmit NFSe (`TransmitNfseUseCase`)

**Module:** M4 — Fiscal: NFSe ([module spec](../m4-fiscal-nfse.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 6 — Serviços (doc §14)

## Functional requirement

Extensibility: new standards added as adapters without changing the module's core (§5.1). Status: Draft → Sent → Authorized → Cancelled, with timestamps and protocols (§5.2). For municipalities without homologated integration, the system generates the XML in the correct standard and guides the user to a manual upload on the municipality's portal — avoiding a total block of the operation (§5.2).

## Description

Submits a `DRAFT` `NfseDocument` to the municipality via the standard-specific `IssueNfsePort` adapter selected from that municipality's `MunicipalityIntegration` configuration (UC-M4-01). On success, the document moves through `SENT` → `AUTHORIZED` with a protocol and timestamp. When the municipality is registered as not homologated, this use case does not attempt transmission — it instead produces the standard-correct XML and hands back guided instructions for manual upload on the municipality's own portal, so the operation is never fully blocked.

## Port signature

```java
public interface TransmitNfseUseCase {
    NfseTransmissionResult execute(TransmitNfseCommand command);
}
```

`TransmitNfseCommand`: `nfseId`. `NfseTransmissionResult` is one of: authorized (protocol, timestamp), rejected (reason), or guided-manual-upload (generated XML + instructions) — see [m4-fiscal-nfse.md](../m4-fiscal-nfse.md) Notes on this being a first-class outcome, not an error state.

## Outbound ports required

- `NfseRepositoryPort`
- `MunicipalityIntegrationRepositoryPort` (to resolve standard/homologation status)
- `IssueNfsePort` (dispatched to the standard-specific adapter: ABRASF, NFS-e Nacional, ISS.net, Betha)
- `GenerateGuidedManualUploadPort` (non-homologated fallback)
- `XmlObjectStoragePort` (shared with M2)

## REST endpoint

No dedicated endpoint listed in the module spec's adapter table; transmission is triggered as part of `POST /api/nfse/rps/convert` completing, or as a follow-up action on a `DRAFT` document. Flag this gap for the frontend/API design pass.

## Domain entities touched

- `NfseDocument` (`status` transition, `protocol`, timestamps)
- `MunicipalityIntegration` (read-only)

## Acceptance criteria

- [ ] A homologated municipality's document is signed and transmitted via the correct `IssueNfsePort` adapter for its registered standard.
- [ ] A non-homologated municipality's document produces a correctly-formatted XML and manual-upload instructions instead of failing.
- [ ] Successful transmission records a protocol and moves `status` to `AUTHORIZED`; failure keeps the document transmittable again (no stuck intermediate state).
- [ ] Adding a new municipal standard adapter requires no change to this use case's logic — only a new `IssueNfsePort` implementation (doc §13).
- [ ] The transmitted XML is stored via `XmlObjectStoragePort`, with only a reference kept on the `NfseDocument`.

## Dependencies

- **Depends on:** UC-M4-01 (`register-municipality-integration`), UC-M4-03 (`convert-rps-to-nfse`), M2 — `CalculateTaxUseCase` (../m2-fiscal-nfe/uc-02-calculate-tax.md) for tax totals already resolved at RPS issuance and carried through transmission.
- **Blocks:** UC-M4-05 (`cancel-nfse`); `finance`'s accounts-receivable generation on authorized NFSe.

## Implementation notes

- **Endpoint (proposed).** `POST /api/nfse/{id}/transmit`, no body, always `200` with `{"outcome": ..., ...}`. `outcome` is `AUTHORIZED` (`protocol`, `authorizedAt`), `REJECTED` (`rejectionReason`) or `MANUAL_UPLOAD_REQUIRED` (`xml`, `instructions`); only the fields of that outcome are present. A rejection and a manual-upload hand-off are decided results of the call, not failures of it. Convert (`POST /api/nfse/rps/convert`) does **not** transmit; transmission is the follow-up action on a `DRAFT`. Errors: `404` unknown id; `409` document not `DRAFT` (an RPS must be converted first; an already `AUTHORIZED` one is never re-issued), no integration registered for the provider's municipality, or a homologated municipality whose standard has no adapter; `503` municipality did not answer.
- **Municipality.** The integration is looked up by the *provider's* municipality (`providerMunicipalityIbgeCode`, the one the NFSe is numbered under), not the ISS municipality.
- **Adapter dispatch (AC4).** `IssueNfsePort` exposes `standard()`; `TransmitNfseService` indexes every `IssueNfsePort` bean by it. A new standard is one more bean - no change to the use case. Two beans for one standard fail at startup. The adapter signs inside `issue(...)` and returns either `authorized(protocol, authorizedAt, signedXml)` or `rejected(reason)`; it throws `NfseMunicipalityUnavailableException` when the municipality cannot be reached. **No concrete `IssueNfsePort` adapter ships with this ticket** (ABRASF / NFS-e Nacional / ISS.net / Betha webservice clients are separate work): until one exists, a homologated municipality answers `409` and the document stays `DRAFT`, and non-homologated municipalities work fully via the guided path.
- **State machine (AC3).** The whole attempt is one transaction holding the row lock (`findByIdForUpdate`): `DRAFT -> SENT` (`NfseDocument.send`), then `authorize(protocol, at, xmlRef)` or `reject(reason)`. A rejection commits and returns the document to `DRAFT` with `lastRejectionReason` (cleared on the next attempt); a thrown exception (including an unreachable municipality) rolls everything back to the original `DRAFT`. So `SENT` is never visible to other transactions and nothing can be left stuck, and two concurrent transmits of the same NFSe are serialized. Trade-off: a DB connection and row lock are held for the duration of the webservice call.
- **XML (AC5).** The signed XML returned by the adapter is stored through `XmlObjectStoragePort`; `NfseDocument.xmlReference` is the only thing kept on the row. Only the authorized XML is stored; the guided-upload XML is returned in the response and not persisted.
- **Guided manual upload (AC2).** `GuidedManualUploadAdapter` renders an ABRASF 2.04-shaped `GerarNfseEnvio` (provider CNPJ/IM from the company, tomador, service, ISS) and step-by-step instructions naming the standard/version, the required certificate type and the municipality's required fields. Known limit: that one layout is used for every standard (the standard only shapes the instructions), and it is not schema-validated - like M2's `NfeXmlRenderer`. Per-standard layouts and the full XSD are follow-ups. Document and integration are not modified.
- **Known edge.** If the municipality authorizes but storing the XML then fails, the transaction rolls back to `DRAFT` although the municipality holds an authorized NFSe; a retry would issue it twice. A real adapter should make the municipality call idempotent (e.g. look the RPS up before re-sending) - an RPS-number lookup is the usual ABRASF way.
- Migration `V74` adds `sent_at`, `protocol`, `authorized_at`, `xml_reference`, `last_rejection_reason` to `nfse_documents`.
