package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeResolution;
import br.gravita.core.domain.tax.InboundNfeResolution.ItemResolution;
import br.gravita.core.ports.inbound.tax.GetInboundNfeConferenceQuery;
import br.gravita.core.ports.inbound.tax.GetInboundNfeConferenceUseCase;
import br.gravita.core.ports.inbound.tax.InboundNfeConference;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@UseCase
public class GetInboundNfeConferenceService implements GetInboundNfeConferenceUseCase {

	private final OwnedInboundNfeLoader ownedInboundNfeLoader;
	private final InboundNfeResolver inboundNfeResolver;
	private final ProductRepositoryPort productRepositoryPort;

	public GetInboundNfeConferenceService(final OwnedInboundNfeLoader ownedInboundNfeLoader,
			final InboundNfeResolver inboundNfeResolver, final ProductRepositoryPort productRepositoryPort) {
		this.ownedInboundNfeLoader = ownedInboundNfeLoader;
		this.inboundNfeResolver = inboundNfeResolver;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public InboundNfeConference execute(final GetInboundNfeConferenceQuery query) {
		final InboundNfe inboundNfe = ownedInboundNfeLoader.load(query.inboundNfeId(), query.callerId());
		final InboundNfeResolution resolution = inboundNfeResolver.resolve(inboundNfe);
		final Map<UUID, String> productLabels = productLabels(resolution);
		final List<InboundNfeConference.Item> items = resolution.items().stream()
				.map(item -> toItem(item, productLabels))
				.toList();
		return new InboundNfeConference(inboundNfe.getId().value(), inboundNfe.getSupplierDocument().number(),
				inboundNfe.getSupplierName(), resolution.isSupplierResolved(),
				resolution.supplierId().map(SupplierId::value).orElse(null), items);
	}

	private Map<UUID, String> productLabels(final InboundNfeResolution resolution) {
		final List<UUID> productIds = resolution.items().stream()
				.flatMap(item -> item.link().stream())
				.map(SupplierProductCatalogLink::getProductId)
				.distinct()
				.toList();
		return productRepositoryPort.findAllByIds(productIds).stream()
				.collect(Collectors.toMap(ProductDomain::getId, ProductDomain::getInternalCode, (first, second) -> first));
	}

	private InboundNfeConference.Item toItem(final ItemResolution resolution, final Map<UUID, String> productLabels) {
		final InboundNfeItem item = resolution.item();
		final SupplierProductCatalogLink link = resolution.link().orElse(null);
		return new InboundNfeConference.Item(resolution.index(), item.supplierProductCode(), item.description(), item.ncm(),
				item.unit(), item.quantity(), item.unitValue(),
				link == null ? null : link.getProductId(),
				link == null ? null : productLabels.get(link.getProductId()),
				link == null ? null : link.getLastPurchaseUnitValue(),
				link == null ? null : link.getLastPurchaseAt(),
				link == null ? null : link.priceDelta(item.unitValue()), item.barcode(), item.cest(),
				link == null ? suggestByGtin(item) : null);
	}

	/** An unlinked item whose GTIN is already in the catalog probably is that product; the operator still confirms it. */
	private InboundNfeConference.ProductSuggestion suggestByGtin(final InboundNfeItem item) {
		if (item.barcode() == null) {
			return null;
		}
		return productRepositoryPort.findByBarcode(item.barcode())
				.map(product -> new InboundNfeConference.ProductSuggestion(product.getId(), product.getInternalCode()))
				.orElse(null);
	}
}
