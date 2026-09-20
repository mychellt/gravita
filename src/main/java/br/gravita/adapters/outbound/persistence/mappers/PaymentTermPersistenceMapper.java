package br.gravita.adapters.outbound.persistence.mappers;

import br.gravita.adapters.outbound.persistence.entities.PaymentTermJpaEntity;
import br.gravita.core.domain.PaymentTermDomain;

public class PaymentTermPersistenceMapper {

	public PaymentTermDomain toDomain(PaymentTermJpaEntity entity) {
		return PaymentTermDomain.builder()
				.id(entity.getId())
				.name(entity.getName())
				.installmentIntervalsDays(entity.getInstallmentIntervalsDays())
				.build();
	}

	public PaymentTermJpaEntity toEntity(PaymentTermDomain domain) {
		return PaymentTermJpaEntity.builder()
				.id(domain.getId())
				.name(domain.getName())
				.installmentIntervalsDays(domain.getInstallmentIntervalsDays())
				.build();
	}
}
