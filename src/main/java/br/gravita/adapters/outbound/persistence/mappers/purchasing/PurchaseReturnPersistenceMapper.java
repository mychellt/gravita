package br.gravita.adapters.outbound.persistence.mappers.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReturnItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReturnJpaEntity;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReturn;
import br.gravita.core.domain.purchasing.PurchaseReturnId;
import br.gravita.core.domain.purchasing.PurchaseReturnItem;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PurchaseReturnPersistenceMapper {

	PurchaseReturn map(final PurchaseReturnJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "receiptId", source = "receiptId.value")
	PurchaseReturnJpaEntity map(final PurchaseReturn domain);

	PurchaseReturnItem map(final PurchaseReturnItemEmbeddable embeddable);

	PurchaseReturnItemEmbeddable map(final PurchaseReturnItem item);

	@Mapping(target = "value", source = "id")
	PurchaseReturnId mapPurchaseReturnId(final UUID id);

	@Mapping(target = "value", source = "id")
	PurchaseReceiptId mapPurchaseReceiptId(final UUID id);
}
