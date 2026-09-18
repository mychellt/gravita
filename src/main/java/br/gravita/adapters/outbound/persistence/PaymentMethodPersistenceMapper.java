package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.PaymentMethodJpaEntity;
import br.gravita.core.domain.PaymentMethodDomain;

class PaymentMethodPersistenceMapper {

	PaymentMethodDomain toDomain(PaymentMethodJpaEntity entity) {
		return PaymentMethodDomain.builder()
				.id(entity.getId())
				.name(entity.getName())
				.type(entity.getType())
				.build();
	}

	PaymentMethodJpaEntity toEntity(PaymentMethodDomain domain) {
		return PaymentMethodJpaEntity.builder()
				.id(domain.getId())
				.name(domain.getName())
				.type(domain.getType())
				.build();
	}
}
