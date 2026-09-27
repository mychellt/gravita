package br.gravita.core.ports.inbound.tax;

/**
 * UC-M2-03: signs and submits a {@code QUEUED} {@link
 * br.gravita.core.domain.tax.NfeDocument} to SEFAZ, then renders/stores/
 * e-mails the DANFE on authorization. Invoked by the transmission queue
 * consumer, never directly by a user action.
 */
public interface TransmitNfeUseCase {

	TransmissionResult execute(TransmitNfeCommand command);
}
