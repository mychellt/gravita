package br.gravita.masterdata.application.port.out;

import br.gravita.masterdata.domain.model.PriceTable;
import br.gravita.masterdata.domain.model.PriceTableId;
import java.util.Optional;

public interface PriceTableRepositoryPort {
	PriceTable save(PriceTable priceTable);
	Optional<PriceTable> findById(PriceTableId id);
}
