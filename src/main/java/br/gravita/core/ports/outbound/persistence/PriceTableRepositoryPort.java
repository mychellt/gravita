package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import java.util.Optional;

public interface PriceTableRepositoryPort {
	PriceTable save(PriceTable priceTable);
	Optional<PriceTable> findById(PriceTableId id);
}
