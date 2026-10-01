package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.CommissionRateJpaEntity;
import br.gravita.core.domain.sales.CommissionRate;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CommissionRatePersistenceMapper {

	CommissionRate map(final CommissionRateJpaEntity entity);
}
