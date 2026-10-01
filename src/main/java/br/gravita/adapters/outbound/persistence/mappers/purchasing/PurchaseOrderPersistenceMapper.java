package br.gravita.adapters.outbound.persistence.mappers.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseOrderItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseOrderJpaEntity;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PurchaseOrderPersistenceMapper {

	PurchaseOrder map(final PurchaseOrderJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "requestId", source = "requestId.value")
	@Mapping(target = "supplierId", source = "supplierId.value")
	PurchaseOrderJpaEntity map(final PurchaseOrder domain);

	PurchaseOrderItem map(final PurchaseOrderItemEmbeddable embeddable);

	PurchaseOrderItemEmbeddable map(final PurchaseOrderItem item);

	@Mapping(target = "value", source = "id")
	PurchaseOrderId mapPurchaseOrderId(final UUID id);

	@Mapping(target = "value", source = "id")
	PurchaseRequestId mapPurchaseRequestId(final UUID id);

	@Mapping(target = "value", source = "id")
	SupplierId mapSupplierId(final UUID id);
}
