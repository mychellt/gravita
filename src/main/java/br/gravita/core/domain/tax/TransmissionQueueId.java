package br.gravita.core.domain.tax;

import java.util.UUID;

/**
 * Common identity contract for anything that can be pushed onto the shared
 * {@link br.gravita.core.ports.outbound.tax.TransmissionQueuePort} (doc
 * §13's "fila de transmissão única", reused by M2/M3/M4). Implemented by
 * {@link NfceSaleId} and {@link NfeDocumentId} so the port doesn't need one
 * overload per document type.
 */
public interface TransmissionQueueId {

	UUID value();
}
