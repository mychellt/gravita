package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.NfceSaleId;

/**
 * Contingency queue for sales SEFAZ-UF couldn't authorize online (AC2).
 * Consumed later by {@code SyncContingencySalesUseCase} (UC-08); not built
 * yet, so this port only accepts entries for now.
 */
public interface TransmissionQueuePort {

	void enqueue(NfceSaleId nfceSaleId);
}
