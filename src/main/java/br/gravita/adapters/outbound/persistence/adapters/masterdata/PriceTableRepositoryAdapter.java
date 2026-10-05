package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.PriceTableJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.PriceTablePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.PriceTableJpaRepository;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.annotations.PersistenceAdapter;
import java.util.Optional;

@PersistenceAdapter
class PriceTableRepositoryAdapter implements PriceTableRepositoryPort {

	private final PriceTableJpaRepository jpaRepository;
	private final PriceTablePersistenceMapper mapper;

	PriceTableRepositoryAdapter(final PriceTableJpaRepository jpaRepository, final PriceTablePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PriceTable save(final PriceTable priceTable) {
		final PriceTableJpaEntity entity = mapper.map(priceTable);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final PriceTableJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<PriceTable> findById(final PriceTableId id) {
		return jpaRepository.findById(id.value()).map(entity -> mapper.map(entity));
	}
}
