package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceSaleJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.PaymentEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.tax.SaleItemEmbeddable;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.Payment;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.SaleItem;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface NfceSalePersistenceMapper {

	@Mapping(target = "createdAt", source = "registeredAt")
	NfceSale map(final NfceSaleJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "sessionId", source = "sessionId.value")
	@Mapping(target = "registeredAt", source = "createdAt")
	@Mapping(target = "createdAt", ignore = true)
	NfceSaleJpaEntity map(final NfceSale domain);

	SaleItem map(final SaleItemEmbeddable embeddable);

	SaleItemEmbeddable map(final SaleItem item);

	Payment map(final PaymentEmbeddable embeddable);

	PaymentEmbeddable map(final Payment payment);

	@Mapping(target = "value", source = "id")
	NfceSaleId mapNfceSaleId(final UUID id);

	@Mapping(target = "value", source = "id")
	PosSessionId mapPosSessionId(final UUID id);
}
