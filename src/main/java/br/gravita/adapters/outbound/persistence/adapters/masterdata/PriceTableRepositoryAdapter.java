package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.PriceTableJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.PriceTablePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.PriceTableJpaRepository;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.shared.PersistenceAdapter;
import java.util.Optional;

@PersistenceAdapter
class PriceTableRepositoryAdapter implements PriceTableRepositoryPort {

	private final PriceTableJpaRepository jpaRepository;
	private final PriceTablePersistenceMapper mapper = new PriceTablePersistenceMapper();

	PriceTableRepositoryAdapter(PriceTableJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public PriceTable save(PriceTable priceTable) {
		PriceTableJpaEntity entity = mapper.toEntity(priceTable);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		PriceTableJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<PriceTable> findById(PriceTableId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}
}
