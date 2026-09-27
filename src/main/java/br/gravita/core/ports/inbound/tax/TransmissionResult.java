package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfeDocumentStatus;

/**
 * The decided outcome of a transmission attempt (UC-M2-03) - {@code status}
 * is always {@code AUTHORIZED} or {@code REJECTED}. A timeout is not a
 * decided outcome: {@code TransmitNfeUseCase} lets
 * {@link br.gravita.core.domain.tax.SefazUnavailableException} propagate
 * instead, so the queue consumer can apply its own retry/backoff/contingency
 * policy.
 */
public record TransmissionResult(NfeDocumentStatus status, String protocol, String rejectionReason) {
}
