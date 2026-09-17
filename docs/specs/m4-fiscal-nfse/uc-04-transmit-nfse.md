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
