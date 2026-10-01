package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.SettlementJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.SettlementPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.CashFlowQueryRepository;
import br.gravita.adapters.outbound.persistence.repositories.finance.SettlementJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@PersistenceAdapter
class SettlementRepositoryAdapter implements SettlementRepositoryPort {

	private final SettlementJpaRepository jpaRepository;
	private final CashFlowQueryRepository cashFlowQueryRepository;
	private final SettlementPersistenceMapper mapper;

	SettlementRepositoryAdapter(SettlementJpaRepository jpaRepository,
			CashFlowQueryRepository cashFlowQueryRepository, SettlementPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.cashFlowQueryRepository = cashFlowQueryRepository;
		this.mapper = mapper;
	}

	@Override
	public Settlement save(Settlement settlement) {
		SettlementJpaEntity entity = mapper.map(settlement);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.map(jpaRepository.save(entity));
	}

	@Override
	public List<Settlement> findByReceivableId(ReceivableId receivableId) {
		return jpaRepository.findByReceivableIdOrderByTimestampAscCreatedAtAsc(receivableId.value()).stream()
				.map(mapper::map).toList();
	}

	@Override
	public List<Settlement> findByPayableId(PayableId payableId) {
		return jpaRepository.findByPayableIdOrderByTimestampAscCreatedAtAsc(payableId.value()).stream()
				.map(mapper::map).toList();
	}

	@Override
	public List<Settlement> findByReceivableIds(Collection<ReceivableId> receivableIds) {
		if (receivableIds.isEmpty()) {
			return List.of();
		}
		return jpaRepository
				.findByReceivableIdInOrderByTimestampAscCreatedAtAsc(
						receivableIds.stream().map(ReceivableId::value).toList())
				.stream().map(mapper::map).toList();
	}

	@Override
	public List<Settlement> findRealizedBetween(Instant from, Instant until, CashFlowFilter filter) {
		return cashFlowQueryRepository.findRealizedSettlements(from, until, filter).stream().map(mapper::map)
				.toList();
	}
}
