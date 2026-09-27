package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfeDocumentId;

/**
 * Transmission queue, shared across M2/M3/M4 (module spec §13). The NFCe
 * side is a contingency queue for sales SEFAZ-UF couldn't authorize online
 * (UC-M3-04 AC2); the NFe side (UC-M2-01) queues every issued document for
 * asynchronous transmission. Consumed later by {@code SyncContingencySalesUseCase}
 * (UC-08) and UC-M2-03 respectively - neither is built yet, so this port
 * only accepts entries for now.
 */
public interface TransmissionQueuePort {

	void enqueue(NfceSaleId nfceSaleId);

	void enqueue(NfeDocumentId nfeDocumentId);
}
