package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.PaymentMethodJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.PaymentMethodPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.PaymentMethodJpaRepository;
import br.gravita.core.domain.PaymentMethodDomain;
import br.gravita.core.ports.outbound.persistence.PaymentMethodRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class PaymentMethodRepositoryAdapter implements PaymentMethodRepositoryPort {

	private final PaymentMethodJpaRepository jpaRepository;
	private final PaymentMethodPersistenceMapper mapper = new PaymentMethodPersistenceMapper();

	PaymentMethodRepositoryAdapter(PaymentMethodJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public PaymentMethodDomain save(PaymentMethodDomain model) {
		PaymentMethodJpaEntity saved = jpaRepository.save(mapper.toEntity(model));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<PaymentMethodDomain> get(UUID id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<PaymentMethodDomain> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public void deleteById(UUID id) {
		jpaRepository.deleteById(id);
	}
}
