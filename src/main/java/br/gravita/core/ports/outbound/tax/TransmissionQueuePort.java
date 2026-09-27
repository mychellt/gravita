package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.NfceSaleId;

public interface TransmissionQueuePort {

	void enqueue(NfceSaleId nfceSaleId);
}
