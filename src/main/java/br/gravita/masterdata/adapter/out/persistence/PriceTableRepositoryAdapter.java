package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.application.port.out.PriceTableRepositoryPort;
import br.gravita.masterdata.domain.model.PriceTable;
import br.gravita.masterdata.domain.model.PriceTableId;
import br.gravita.shared.PersistenceAdapter;
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
