package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.SettlementJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.SettlementPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.SettlementJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.util.List;

@PersistenceAdapter
class SettlementRepositoryAdapter implements SettlementRepositoryPort {

	private final SettlementJpaRepository jpaRepository;
	private final SettlementPersistenceMapper mapper;

	SettlementRepositoryAdapter(SettlementJpaRepository jpaRepository, SettlementPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Settlement save(Settlement settlement) {
		SettlementJpaEntity entity = mapper.toEntity(settlement);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	public List<Settlement> findByReceivableId(ReceivableId receivableId) {
		return jpaRepository.findByReceivableIdOrderByTimestampAscCreatedAtAsc(receivableId.value()).stream()
				.map(mapper::toDomain).toList();
	}
}
