package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.PaymentTermJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.PaymentTermPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.PaymentTermJpaRepository;
import br.gravita.core.domain.PaymentTermDomain;
import br.gravita.core.ports.outbound.persistence.PaymentTermRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class PaymentTermRepositoryAdapter implements PaymentTermRepositoryPort {

	private final PaymentTermJpaRepository jpaRepository;
	private final PaymentTermPersistenceMapper mapper;

	PaymentTermRepositoryAdapter(PaymentTermJpaRepository jpaRepository, PaymentTermPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PaymentTermDomain save(PaymentTermDomain model) {
		PaymentTermJpaEntity saved = jpaRepository.save(mapper.map(model));
		return mapper.map(saved);
	}

	@Override
	public Optional<PaymentTermDomain> get(UUID id) {
		return jpaRepository.findById(id).map(mapper::map);
	}

	@Override
	public List<PaymentTermDomain> findAll() {
		return jpaRepository.findAll().stream().map(mapper::map).toList();
	}

	@Override
	public void deleteById(UUID id) {
		jpaRepository.deleteById(id);
	}
}
