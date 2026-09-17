# UC-M4-03 — Convert RPS to NFSe (`ConvertRpsToNfseUseCase`)

**Module:** M4 — Fiscal: NFSe ([module spec](../m4-fiscal-nfse.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 6 — Serviços (doc §14)

## Functional requirement

RPS converted in batch or individually (§5.2). Status: Draft → Sent → Authorized → Cancelled, with timestamps and protocols (§5.2).

## Description

Converts one or more issued RPS into `NfseDocument`s ready for transmission. Individual conversion targets a single RPS; batch conversion processes a set (e.g. all pending RPS for a period). Conversion assigns the NFSe `number`/`series` (scoped per company+municipality, independent of the NFe series) and moves the document to `DRAFT`, awaiting `TransmitNfseUseCase`.

## Port signature

```java
public interface ConvertRpsToNfseUseCase {
    List<NfseId> execute(ConvertRpsToNfseCommand command);
}
```

`ConvertRpsToNfseCommand`: `rpsIds` (one or more). Returns the resulting `NfseDocument` ids, in `DRAFT` status.

## Outbound ports required

- `NfseRepositoryPort`

## REST endpoint

`POST /api/nfse/rps/convert` (accepts one or many RPS ids — batch or individual per §5.2)

## Domain entities touched

- `NfseDocument` (`series`, `number`, `status` transition into `DRAFT`)

## Acceptance criteria

- [ ] Both single-RPS and multi-RPS (batch) conversion are supported through the same use case.
- [ ] Each converted document receives a number scoped to `(company, municipality)`, independent of the NFe numbering series.
- [ ] Conversion sets `status = DRAFT` with a timestamp; it does not itself transmit to the municipality.
- [ ] Converting the same RPS twice does not produce two `NfseDocument`s.

## Dependencies

- **Depends on:** UC-M4-02 (`issue-rps`).
- **Blocks:** UC-M4-04 (`transmit-nfse`).
