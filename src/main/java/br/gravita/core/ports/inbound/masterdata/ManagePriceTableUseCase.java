package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.PriceTableId;

public interface ManagePriceTableUseCase {
	PriceTableId execute(UpsertPriceTableCommand command);
}
