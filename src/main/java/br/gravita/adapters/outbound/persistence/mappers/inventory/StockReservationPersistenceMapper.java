package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockReservationJpaEntity;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StockReservationPersistenceMapper {

	StockReservation map(final StockReservationJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	StockReservationJpaEntity map(final StockReservation domain);

	@Mapping(target = "value", source = "id")
	StockReservationId mapStockReservationId(final UUID id);
}
