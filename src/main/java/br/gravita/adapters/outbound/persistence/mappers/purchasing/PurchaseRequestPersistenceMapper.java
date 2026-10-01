package br.gravita.adapters.outbound.persistence.mappers.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseRequestItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseRequestJpaEntity;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PurchaseRequestPersistenceMapper {

	@Mapping(target = "id", source = "id.value")
	PurchaseRequestJpaEntity map(final PurchaseRequest domain);

	PurchaseRequest map(final PurchaseRequestJpaEntity entity);

	PurchaseRequestItem map(final PurchaseRequestItemEmbeddable embeddable);

	PurchaseRequestItemEmbeddable map(final PurchaseRequestItem item);

	@Mapping(target = "value", source = "id")
	PurchaseRequestId map(final UUID id);
}
