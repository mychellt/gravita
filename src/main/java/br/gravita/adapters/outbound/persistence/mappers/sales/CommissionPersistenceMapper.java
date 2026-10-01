package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.CommissionJpaEntity;
import br.gravita.core.domain.sales.Commission;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CommissionPersistenceMapper {

	@Mapping(target = "id.value", source = "id")
	@Mapping(target = "orderId.value", source = "salesOrderId")
	Commission map(final CommissionJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "salesOrderId", source = "orderId.value")
	CommissionJpaEntity map(final Commission domain);
}
