package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.InternalCashBoxJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.finance.InternalCashMovementJpaEntity;
import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementId;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.finance.InternalCashBoxId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface InternalCashBoxPersistenceMapper {

	default InternalCashBox toDomain(final InternalCashBoxJpaEntity entity) {
		return InternalCashBox.of(InternalCashBoxId.of(entity.getId()), entity.getBalance());
	}

	default InternalCashBoxJpaEntity toEntity(final InternalCashBox domain) {
		return InternalCashBoxJpaEntity.builder()
				.id(domain.getId().value())
				.balance(domain.getBalance())
				.build();
	}

	default CashMovement toDomain(final InternalCashMovementJpaEntity entity) {
		return CashMovement.of(CashMovementId.of(entity.getId()), InternalCashBoxId.of(entity.getCashBoxId()),
				entity.getDirection(), entity.getAmount(), entity.getJustification(), entity.getTimestamp());
	}

	default InternalCashMovementJpaEntity toEntity(final CashMovement domain) {
		return InternalCashMovementJpaEntity.builder()
				.id(domain.getId().value())
				.cashBoxId(domain.getCashBoxId().value())
				.direction(domain.getDirection())
				.amount(domain.getAmount())
				.justification(domain.getJustification())
				.timestamp(domain.getTimestamp())
				.build();
	}
}
