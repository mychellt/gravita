package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.SettlementJpaEntity;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SettlementPersistenceMapper {

	default Settlement toDomain(final SettlementJpaEntity entity) {
		if (entity.getPayableId() != null) {
			return Settlement.ofPayable(SettlementId.of(entity.getId()), PayableId.of(entity.getPayableId()),
					entity.getAmount(), entity.getInterest(), entity.getFine(), entity.getDiscount(),
					entity.getSurcharge(), entity.getMethod(), entity.getTimestamp());
		}
		return Settlement.of(SettlementId.of(entity.getId()), ReceivableId.of(entity.getReceivableId()),
				entity.getAmount(), entity.getInterest(), entity.getFine(), entity.getDiscount(),
				entity.getSurcharge(), entity.getMethod(), entity.getTimestamp());
	}

	default SettlementJpaEntity toEntity(final Settlement domain) {
		return SettlementJpaEntity.builder()
				.id(domain.getId().value())
				.receivableId(domain.getReceivableId() == null ? null : domain.getReceivableId().value())
				.payableId(domain.getPayableId() == null ? null : domain.getPayableId().value())
				.amount(domain.getAmount())
				.interest(domain.getInterest())
				.fine(domain.getFine())
				.discount(domain.getDiscount())
				.surcharge(domain.getSurcharge())
				.method(domain.getMethod())
				.timestamp(domain.getTimestamp())
				.build();
	}
}
