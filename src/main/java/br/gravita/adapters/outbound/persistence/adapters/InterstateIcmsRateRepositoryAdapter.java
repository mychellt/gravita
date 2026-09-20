package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.mappers.InterstateIcmsRatePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.InterstateIcmsRateJpaRepository;
import br.gravita.core.domain.InterstateIcmsRateDomain;
import br.gravita.core.ports.outbound.persistence.InterstateIcmsRateRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class InterstateIcmsRateRepositoryAdapter implements InterstateIcmsRateRepositoryPort {

	private final InterstateIcmsRateJpaRepository jpaRepository;
	private final InterstateIcmsRatePersistenceMapper mapper = new InterstateIcmsRatePersistenceMapper();

	InterstateIcmsRateRepositoryAdapter(InterstateIcmsRateJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<InterstateIcmsRateDomain> get(UUID id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<InterstateIcmsRateDomain> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public Optional<InterstateIcmsRateDomain> findByOriginStateAndDestinationState(String originState, String destinationState) {
		return jpaRepository.findByOriginStateAndDestinationState(originState, destinationState).map(mapper::toDomain);
	}

	@Override
	public List<InterstateIcmsRateDomain> saveAll(List<InterstateIcmsRateDomain> rates) {
		return jpaRepository.saveAll(rates.stream().map(mapper::toEntity).toList())
				.stream().map(mapper::toDomain).toList();
	}
}
