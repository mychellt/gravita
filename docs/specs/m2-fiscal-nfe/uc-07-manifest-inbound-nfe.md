# UC-M2-07 — Manifest Inbound NFe (`ManifestInboundNfeUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.2: "Manifestação do destinatário: confirmation, unknown or operation-not-performed, for inbound NFe."

## Description

Triggered by a user reviewing an NFe issued against their company by a third party (typically a supplier). Records the recipient's manifestation — confirming the operation, declaring it unknown, or declaring the operation was not actually performed — and forwards it to SEFAZ. This is independent of whether the inbound NFe has been entered into the system via [UC-M2-08](uc-08-import-supplier-nfe-xml.md)/[UC-M2-09](uc-09-enter-inbound-nfe-manually.md).

## Port signature

```java
public interface ManifestInboundNfeUseCase {
    InboundManifestation execute(ManifestInboundNfeCommand command);
}
```

`ManifestInboundNfeCommand` fields: `accessKey`, `type` (`CONFIRMED` / `UNKNOWN` / `OPERATION_NOT_PERFORMED`). Returns the created `InboundManifestation`.

## Outbound ports required

- `InboundNfeRepositoryPort`
- `SubmitToSefazPort`

## REST endpoint

`POST /api/nfe/inbound/manifestation`

## Domain entities touched

- `InboundManifestation` (created)
- `InboundNfe` (referenced by access key)

## Acceptance criteria

- [ ] Accepts exactly the three manifestation types from the source doc; no others.
- [ ] Manifestation is recorded with a timestamp and forwarded to SEFAZ.
- [ ] Works by access key alone — doesn't require the inbound NFe to already be entered in the system.

## Dependencies

- **Depends on:** None (can precede or follow inbound entry).
- **Blocks:** —
