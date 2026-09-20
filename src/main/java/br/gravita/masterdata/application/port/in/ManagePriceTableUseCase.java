package br.gravita.masterdata.application.port.in;

import br.gravita.masterdata.domain.model.PriceTableId;

public interface ManagePriceTableUseCase {
	PriceTableId execute(UpsertPriceTableCommand command);
}
