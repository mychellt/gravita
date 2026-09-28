package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.PixChargeJpaEntity;
import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeId;
import br.gravita.core.domain.finance.ReceivableId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PixChargePersistenceMapper {

	default PixCharge toDomain(final PixChargeJpaEntity entity) {
		return PixCharge.of(PixChargeId.of(entity.getId()), ReceivableId.of(entity.getReceivableId()),
				entity.getDynamicQrPayload(), entity.getAmount(), entity.getDueDate(), entity.getExpiresAt(),
				entity.getStatus());
	}

	default PixChargeJpaEntity toEntity(final PixCharge domain) {
		return PixChargeJpaEntity.builder()
				.id(domain.getId().value())
				.receivableId(domain.getReceivableId().value())
				.dynamicQrPayload(domain.getDynamicQrPayload())
				.amount(domain.getAmount())
				.dueDate(domain.getDueDate())
				.expiresAt(domain.getExpiresAt())
				.status(domain.getStatus())
				.build();
	}
}
