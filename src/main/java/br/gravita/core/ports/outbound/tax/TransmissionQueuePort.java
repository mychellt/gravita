package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.TransmissionQueueId;

<<<<<<< HEAD
/**
 * Shared transmission/contingency queue (doc §13's "fila de transmissão
 * única"), reused across M2/M3/M4: NFC-e sales that SEFAZ-UF couldn't
 * authorize online (UC-M3-04, AC2), and NFe documents queued after issuance
 * (UC-M2-01, AC7). Accepts any {@link TransmissionQueueId}. Consumed later by
 * {@code SyncContingencySalesUseCase} (NFC-e) and {@code TransmitNfeUseCase}
 * (NFe, UC-M2-03/GRA-103); neither consumer is built yet, so this port only
 * accepts entries for now.
 */
=======
>>>>>>> origin/master
public interface TransmissionQueuePort {

	void enqueue(TransmissionQueueId id);
}
