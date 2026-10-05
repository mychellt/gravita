package br.gravita.adapters.outbound.persistence.mappers.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.QuotationItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.QuotationJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.purchasing.QuotationResponseLineEmbeddable;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.purchasing.QuotationItem;
import br.gravita.core.domain.purchasing.QuotationItemPrice;
import br.gravita.core.domain.purchasing.QuotationResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface QuotationPersistenceMapper {

	@Mapping(target = "responses", source = "responseLines", qualifiedByName = "toResponses")
	Quotation map(final QuotationJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "requestId", source = "requestId.value")
	@Mapping(target = "responseLines", source = "responses", qualifiedByName = "toResponseLines")
	QuotationJpaEntity map(final Quotation domain);

	QuotationItem map(final QuotationItemEmbeddable embeddable);

	QuotationItemEmbeddable map(final QuotationItem item);

	@Mapping(target = "value", source = "id")
	QuotationId mapQuotationId(final UUID id);

	@Mapping(target = "value", source = "id")
	PurchaseRequestId mapPurchaseRequestId(final UUID id);

	@Mapping(target = "value", source = "id")
	SupplierId mapSupplierId(final UUID id);

	static UUID unwrap(final SupplierId id) {
		return id.value();
	}

	@Named("toResponses")
	static List<QuotationResponse> toResponses(final List<QuotationResponseLineEmbeddable> lines) {
		if (lines == null || lines.isEmpty()) {
			return List.of();
		}
		final Map<UUID, List<QuotationResponseLineEmbeddable>> linesBySupplier = lines.stream()
				.collect(Collectors.groupingBy(QuotationResponseLineEmbeddable::getSupplierId, LinkedHashMap::new,
						Collectors.toList()));
		return linesBySupplier.entrySet().stream()
				.map(entry -> new QuotationResponse(
						SupplierId.of(entry.getKey()),
						entry.getValue().stream()
								.map(line -> new QuotationItemPrice(line.getProductId(), line.getUnitPrice()))
								.toList(),
						entry.getValue().get(0).getDeadline()))
				.toList();
	}

	@Named("toResponseLines")
	static List<QuotationResponseLineEmbeddable> toResponseLines(final List<QuotationResponse> responses) {
		return responses.stream()
				.flatMap(response -> response.itemPrices().stream()
						.map(itemPrice -> QuotationResponseLineEmbeddable.builder()
								.supplierId(response.supplierId().value())
								.deadline(response.deadline())
								.productId(itemPrice.productId())
								.unitPrice(itemPrice.unitPrice())
								.build()))
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
