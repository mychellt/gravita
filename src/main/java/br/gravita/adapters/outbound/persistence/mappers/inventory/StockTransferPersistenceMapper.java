package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockTransferJpaEntity;
import br.gravita.core.domain.inventory.StockTransfer;
import br.gravita.core.domain.inventory.StockTransferId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StockTransferPersistenceMapper {

	StockTransfer map(final StockTransferJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	StockTransferJpaEntity map(final StockTransfer domain);

	@Mapping(target = "value", source = "id")
	StockTransferId mapStockTransferId(final UUID id);
}
