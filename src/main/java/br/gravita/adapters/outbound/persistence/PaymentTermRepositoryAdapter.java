package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.PaymentTermJpaEntity;
import br.gravita.core.domain.PaymentTermDomain;
import br.gravita.core.ports.persistence.PaymentTermRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class PaymentTermRepositoryAdapter implements PaymentTermRepositoryPort {

	private final PaymentTermJpaRepository jpaRepository;
	private final PaymentTermPersistenceMapper mapper = new PaymentTermPersistenceMapper();

	PaymentTermRepositoryAdapter(PaymentTermJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public PaymentTermDomain save(PaymentTermDomain model) {
		PaymentTermJpaEntity saved = jpaRepository.save(mapper.toEntity(model));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<PaymentTermDomain> get(UUID id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<PaymentTermDomain> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public void deleteById(UUID id) {
		jpaRepository.deleteById(id);
	}
}
