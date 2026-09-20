package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.adapters.outbound.persistence.entities.tax.ApprovalAlcadaJpaEntity;

class ApprovalAlcadaPersistenceMapper {

	ApprovalAlcadaJpaEntity toEntity(ApprovalAlcada domain) {
		return ApprovalAlcadaJpaEntity.builder()
				.id(domain.getId())
				.module(domain.getModule())
				.thresholdValue(domain.getThresholdValue())
				.thresholdDiscountPercent(domain.getThresholdDiscountPercent())
				.approverProfileId(domain.getApproverProfileId())
				.configuredAt(domain.getConfiguredAt())
				.build();
	}

	ApprovalAlcada toDomain(ApprovalAlcadaJpaEntity entity) {
		return ApprovalAlcada.builder()
				.id(entity.getId())
				.module(entity.getModule())
				.thresholdValue(entity.getThresholdValue())
				.thresholdDiscountPercent(entity.getThresholdDiscountPercent())
				.approverProfileId(entity.getApproverProfileId())
				.configuredAt(entity.getConfiguredAt())
				.build();
	}
}
