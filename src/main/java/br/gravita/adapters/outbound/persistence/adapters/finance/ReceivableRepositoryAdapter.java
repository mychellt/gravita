package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.ReceivableJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.ReceivablePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.CashFlowQueryRepository;
import br.gravita.adapters.outbound.persistence.repositories.finance.ReceivableJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class ReceivableRepositoryAdapter implements ReceivableRepositoryPort {

	private final ReceivableJpaRepository jpaRepository;
	private final CashFlowQueryRepository cashFlowQueryRepository;
	private final ReceivablePersistenceMapper mapper;

	ReceivableRepositoryAdapter(final ReceivableJpaRepository jpaRepository,
			final CashFlowQueryRepository cashFlowQueryRepository, final ReceivablePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.cashFlowQueryRepository = cashFlowQueryRepository;
		this.mapper = mapper;
	}

	@Override
	public Receivable save(final Receivable receivable) {
		final ReceivableJpaEntity entity = mapper.map(receivable);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final ReceivableJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<Receivable> findById(final ReceivableId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public List<Receivable> findByOriginDocumentRef(final UUID originDocumentRef) {
		return jpaRepository.findByOriginDocumentRefOrderByInstallmentNumber(originDocumentRef).stream()
				.map(mapper::map).toList();
	}

	@Override
	public List<Receivable> findByCustomerId(final UUID customerId) {
		return jpaRepository.findByCustomerId(customerId).stream().map(mapper::map).toList();
	}

	@Override
	public List<Receivable> findUnsettledByCustomerId(final UUID customerId) {
		return jpaRepository
				.findByCustomerIdAndStatusIn(customerId,
						List.of(ReceivableStatus.OPEN, ReceivableStatus.PARTIALLY_SETTLED))
				.stream().map(mapper::map).toList();
	}

	@Override
	public List<Receivable> findOutstandingDueUntil(final LocalDate until, final CashFlowFilter filter) {
		if (filter.costCenterId() != null) {
			return List.of();
		}
		return cashFlowQueryRepository.findOutstandingReceivables(until, filter).stream().map(mapper::map)
				.toList();
	}

	@Override
	public List<Receivable> findOutstandingByCustomerDueUntil(final UUID customerId, final LocalDate until) {
		final List<ReceivableStatus> outstanding = List.of(ReceivableStatus.OPEN, ReceivableStatus.PARTIALLY_SETTLED);
		final List<ReceivableJpaEntity> entities = customerId == null
				? jpaRepository.findByStatusInAndDueDateLessThanEqualOrderByDueDateAscIdAsc(outstanding, until)
				: jpaRepository.findByCustomerIdAndStatusInAndDueDateLessThanEqualOrderByDueDateAscIdAsc(customerId,
						outstanding, until);
		return entities.stream().map(mapper::map).toList();
	}
}
