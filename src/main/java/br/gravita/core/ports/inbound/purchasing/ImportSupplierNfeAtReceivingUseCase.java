package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.ConferenceResult;

public interface ImportSupplierNfeAtReceivingUseCase {
	ConferenceResult execute(ImportSupplierNfeAtReceivingCommand command);
}
