package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.PixChargeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.PixChargePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.PixChargeJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeId;
import br.gravita.core.domain.finance.PixChargeStatus;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.ports.outbound.persistence.finance.PixChargeRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
class PixChargeRepositoryAdapter implements PixChargeRepositoryPort {

	private final PixChargeJpaRepository jpaRepository;
	private final PixChargePersistenceMapper mapper;

	PixChargeRepositoryAdapter(PixChargeJpaRepository jpaRepository, PixChargePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PixCharge save(PixCharge pixCharge) {
		PixChargeJpaEntity entity = mapper.toEntity(pixCharge);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	public Optional<PixCharge> findById(PixChargeId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	public List<PixCharge> findByReceivableId(ReceivableId receivableId) {
		return jpaRepository.findByReceivableIdOrderByCreatedAt(receivableId.value()).stream()
				.map(mapper::toDomain).toList();
	}

	@Override
	public List<PixCharge> findPendingExpiredBefore(Instant now) {
		return jpaRepository.findByStatusAndExpiresAtBefore(PixChargeStatus.PENDING, now).stream()
				.map(mapper::toDomain).toList();
	}
}
