package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.CommissionJpaEntity;
import br.gravita.core.domain.sales.Commission;
import br.gravita.core.domain.sales.CommissionId;
import br.gravita.core.domain.sales.SalesOrderId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CommissionPersistenceMapper {

	default Commission toDomain(final CommissionJpaEntity entity) {
		return new Commission(CommissionId.of(entity.getId()), entity.getSalespersonId(), entity.getProductId(),
				SalesOrderId.of(entity.getSalesOrderId()), entity.getRate(), entity.getAmount());
	}

	default CommissionJpaEntity toEntity(final Commission domain) {
		return CommissionJpaEntity.builder()
				.id(domain.id() == null ? null : domain.id().value())
				.salespersonId(domain.salespersonId())
				.productId(domain.productId())
				.salesOrderId(domain.orderId() == null ? null : domain.orderId().value())
				.rate(domain.rate())
				.amount(domain.amount())
				.build();
	}
}
