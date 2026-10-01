package br.gravita.adapters.outbound.persistence.mappers.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.InstallmentTermEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReceiptItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReceiptJpaEntity;
import br.gravita.core.domain.purchasing.InstallmentTerm;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PurchaseReceiptPersistenceMapper {

	@Mapping(target = "receivedItems", source = "items")
	@Mapping(target = "installmentTerms", source = "installments")
	PurchaseReceipt map(final PurchaseReceiptJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "orderId", source = "orderId.value")
	@Mapping(target = "items", source = "receivedItems")
	@Mapping(target = "installments", source = "installmentTerms")
	PurchaseReceiptJpaEntity map(final PurchaseReceipt domain);

	PurchaseReceiptItem map(final PurchaseReceiptItemEmbeddable embeddable);

	PurchaseReceiptItemEmbeddable map(final PurchaseReceiptItem item);

	InstallmentTerm map(final InstallmentTermEmbeddable embeddable);

	InstallmentTermEmbeddable map(final InstallmentTerm term);

	@Mapping(target = "value", source = "id")
	PurchaseReceiptId mapPurchaseReceiptId(final UUID id);

	@Mapping(target = "value", source = "id")
	PurchaseOrderId mapPurchaseOrderId(final UUID id);
}
