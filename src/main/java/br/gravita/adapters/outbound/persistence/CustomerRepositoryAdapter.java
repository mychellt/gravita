package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.CustomerJpaEntity;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.ports.persistence.CustomerRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class CustomerRepositoryAdapter implements CustomerRepositoryPort {

	private final CustomerJpaRepository jpaRepository;
	private final CustomerPersistenceMapper mapper = new CustomerPersistenceMapper();

	CustomerRepositoryAdapter(CustomerJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<CustomerDomain> get(UUID id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<CustomerDomain> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public CustomerDomain save(CustomerDomain model) {
		CustomerJpaEntity entity = mapper.toEntity(model);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		CustomerJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}
}
