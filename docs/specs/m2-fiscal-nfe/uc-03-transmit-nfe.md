# UC-M2-03 — Transmit NFe (`TransmitNfeUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.2: automatic signature with the registered A1 certificate; asynchronous batch submission to SEFAZ with polling and a retry queue on timeout; automatic SVC-AN/SVC-RS contingency when SEFAZ-UF is unavailable; DANFE generated as PDF (portrait default or landscape, company logo, barcode); XML + DANFE sent to the recipient by automatic e-mail on authorization, with manual resend available; XML and DANFE stored for a minimum of 5 years.

## Description

Queue worker triggered after [UC-M2-01](uc-01-issue-nfe.md) enqueues a `QUEUED` document. Signs the document with the company's A1 certificate, submits it to SEFAZ asynchronously, polls for the result, and on timeout retries with exponential backoff (doc §13). Automatically switches to SVC-AN/SVC-RS contingency when the SEFAZ-UF endpoint is unavailable. On authorization, renders the DANFE and e-mails XML+DANFE to the recipient; on rejection, surfaces the SEFAZ rejection reason for correction and re-issuance.

## Port signature

```java
public interface TransmitNfeUseCase {
    TransmissionResult execute(TransmitNfeCommand command);
}
```

`TransmitNfeCommand` fields: `nfeDocumentId`. Returns `TransmissionResult`: `status` (`AUTHORIZED`/`REJECTED`), `protocol`, `rejectionReason` (if any). Invoked by the `TransmissionQueuePort` consumer, not directly by a user action.

## Outbound ports required

- `TransmissionQueuePort`
- `SubmitToSefazPort`
- `GenerateDanfePort`
- `SendFiscalDocumentByEmailPort`
- `XmlObjectStoragePort`
- `NfeRepositoryPort`

## REST endpoint

None dedicated — this is a background queue worker, not a directly invoked endpoint. `GET /api/nfe/{id}/danfe` (download of the already-rendered DANFE) is the only related entry in the module's adapter table, and it's a query, not a trigger for this use case.

## Domain entities touched

- `NfeDocument` (`QUEUED` → `SENT` → `AUTHORIZED`/`REJECTED`)
- `TransmissionQueueEntry` (attempt count, `nextRetryAt`, `contingencyMode`)

## Acceptance criteria

- [ ] Signature is fully automatic with the registered A1 certificate — no user interaction.
- [ ] Submission is asynchronous with polling; a timeout schedules a retry with exponential backoff rather than failing immediately.
- [ ] SVC-AN/SVC-RS contingency activates automatically when SEFAZ-UF is unavailable, without manual intervention.
- [ ] DANFE is generated as PDF, portrait by default, landscape on request, with company logo and barcode.
- [ ] On authorization, XML + DANFE are e-mailed to the recipient automatically; a manual resend action is available afterward.
- [ ] XML and DANFE are stored for at least 5 years and remain downloadable by the user.

## Dependencies

- **Depends on:** [UC-M2-01](uc-01-issue-nfe.md) (produces the `QUEUED` document); [UC-M2-02](uc-02-calculate-tax.md) (tax totals already computed at issuance).
- **Blocks:** [UC-M2-04](uc-04-cancel-nfe.md), [UC-M2-05](uc-05-issue-correction-letter.md) (both require an `AUTHORIZED` document); M7's accounts-receivable generation on invoicing.
