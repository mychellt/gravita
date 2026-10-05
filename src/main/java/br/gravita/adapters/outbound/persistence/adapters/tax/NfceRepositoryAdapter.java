package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceSaleJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.NfceSalePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfceSaleJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
class NfceRepositoryAdapter implements NfceRepositoryPort {

	private final NfceSaleJpaRepository jpaRepository;
	private final NfceSalePersistenceMapper mapper;

	NfceRepositoryAdapter(final NfceSaleJpaRepository jpaRepository, final NfceSalePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public NfceSale save(final NfceSale sale) {
		final NfceSaleJpaEntity entity = mapper.map(sale);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final NfceSaleJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<NfceSale> findById(final NfceSaleId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public Optional<NfceSale> findMostRecent() {
		return jpaRepository.findFirstByOrderByRegisteredAtDesc().map(mapper::map);
	}

	@Override
	public List<NfceSale> findBySessionId(final PosSessionId sessionId) {
		return jpaRepository.findBySessionId(sessionId.value()).stream().map(mapper::map).toList();
	}

	@Override
	public List<NfceSale> findAuthorizedBetween(final Instant from, final Instant to) {
		return jpaRepository
				.findByStatusAndRegisteredAtGreaterThanEqualAndRegisteredAtLessThanOrderByRegisteredAt(
						NfceSaleStatus.AUTHORIZED, from, to)
				.stream().map(mapper::map).toList();
	}
}
