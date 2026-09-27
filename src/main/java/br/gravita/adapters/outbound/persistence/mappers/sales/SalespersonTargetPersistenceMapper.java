package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalespersonTargetJpaEntity;
import br.gravita.core.domain.sales.SalespersonTarget;
import java.time.YearMonth;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SalespersonTargetPersistenceMapper {

	default SalespersonTarget toDomain(final SalespersonTargetJpaEntity entity) {
		return new SalespersonTarget(entity.getSalespersonId(), YearMonth.parse(entity.getMonth()),
				entity.getValueTarget(), entity.getOrderCountTarget());
	}

	default SalespersonTargetJpaEntity toEntity(final SalespersonTarget domain, final UUID id) {
		return SalespersonTargetJpaEntity.builder()
				.id(id)
				.salespersonId(domain.salespersonId())
				.month(domain.month().toString())
				.valueTarget(domain.valueTarget())
				.orderCountTarget(domain.orderCountTarget())
				.build();
	}
}
