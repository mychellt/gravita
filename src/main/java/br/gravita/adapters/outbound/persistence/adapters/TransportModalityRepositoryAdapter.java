package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.TransportModalityJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.TransportModalityPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.TransportModalityJpaRepository;
import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.ports.outbound.persistence.TransportModalityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class TransportModalityRepositoryAdapter implements TransportModalityRepositoryPort {

	private final TransportModalityJpaRepository jpaRepository;
	private final TransportModalityPersistenceMapper mapper;

	TransportModalityRepositoryAdapter(final TransportModalityJpaRepository jpaRepository, final TransportModalityPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public TransportModalityDomain save(final TransportModalityDomain model) {
		final TransportModalityJpaEntity entity = mapper.map(model);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final TransportModalityJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<TransportModalityDomain> get(final UUID id) {
		return jpaRepository.findById(id).map(mapper::map);
	}

	@Override
	public List<TransportModalityDomain> findAll() {
		return jpaRepository.findAllByOrderByNameAsc().stream().map(mapper::map).toList();
	}

	@Override
	public List<TransportModalityDomain> findAllActive() {
		return jpaRepository.findAllByActiveTrueOrderByNameAsc().stream().map(mapper::map).toList();
	}

	@Override
	public Optional<TransportModalityDomain> findActiveByCode(final String code) {
		return jpaRepository.findFirstByCodeAndActiveTrue(code).map(mapper::map);
	}
}
