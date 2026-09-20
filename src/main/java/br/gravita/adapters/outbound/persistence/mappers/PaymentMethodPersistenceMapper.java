package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.PaymentMethodJpaEntity;
import br.gravita.core.domain.PaymentMethodDomain;

public class PaymentMethodPersistenceMapper {

	public PaymentMethodDomain toDomain(PaymentMethodJpaEntity entity) {
		return PaymentMethodDomain.builder()
				.id(entity.getId())
				.name(entity.getName())
				.type(entity.getType())
				.build();
	}

	public PaymentMethodJpaEntity toEntity(PaymentMethodDomain domain) {
		return PaymentMethodJpaEntity.builder()
				.id(domain.getId())
				.name(domain.getName())
				.type(domain.getType())
				.build();
	}
}
