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

## Implementation notes

- `POST /api/nfse/rps/convert` takes `{"rpsIds": ["<uuid>", ...]}` (one id = individual, several = batch) and answers `200` with `{"ids": [...]}` - the `NfseId`s in the order requested. An empty/missing list is `400`; an unknown RPS is `404` and, since the command runs in one transaction, fails the whole batch without consuming any number.
- **Same aggregate.** The RPS is the `NfseDocument` row with `status = RPS` (M4-02), so conversion updates that row in place: `NfseDocument.convertToNfse(series, number, at)` sets `status = DRAFT`, `nfseSeries`, `nfseNumber` and `draftAt` (the timestamp). The RPS series/number stay as they were. Nothing is transmitted (M4-04).
- **Numbering (AC2).** `NfseRepositoryPort.allocateNextNumber(company, municipality)` draws from a new `nfse_number_sequences` table keyed by `(company, municipality)` - the provider's municipality - so it never touches `document_series` (NFe/NFCe/RPS). The row is read with a pessimistic write lock, so concurrent conversions get distinct numbers; the lock and the increment roll back with the transaction. A unique constraint on `(company, municipality, nfse_series, nfse_number)` backs this up. Spec deviation: the ticket lists `NfseRepositoryPort` as the only outbound port, so the sequence lives behind it rather than behind `AllocateDocumentNumberUseCase` (whose series is per company, not per municipality).
- **Series.** The sequence is created lazily on a municipality's first conversion with series `"1"` and next number `1`. There is no API to configure a municipality-specific series yet.
- **Idempotency (AC4).** The document is loaded with `findByIdForUpdate`; anything not in `RPS` status (already `DRAFT` or later) is returned as-is - same id, no new number, no second document. A repeated id inside one batch is converted once. Locking in a stable id order keeps overlapping batches from deadlocking.
- **Known edge.** Two transactions creating the *first* sequence row of the same `(company, municipality)` at the same moment can collide on its unique constraint; the loser fails and the request can be retried.
- Migration `V73` adds `nfse_series`, `nfse_number`, `draft_at` to `nfse_documents` and creates `nfse_number_sequences`.

