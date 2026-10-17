package br.gravita.core.usercases.tax;

import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeResolution;
import br.gravita.core.domain.tax.InboundNfeResolution.ItemResolution;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/**
 * Resolves, on demand, the Supplier (by CNPJ) and the catalog Product links (by supplier + product code) of an
 * {@link InboundNfe}, always inside the NFe's own company. Nothing is stored on the NFe, so a supplier registered or a
 * link created after the import is picked up without any backfill.
 */
@Component
public class InboundNfeResolver {

	private final SupplierRepositoryPort supplierRepositoryPort;
	private final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	public InboundNfeResolver(final SupplierRepositoryPort supplierRepositoryPort,
			final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort) {
		this.supplierRepositoryPort = supplierRepositoryPort;
		this.linkRepositoryPort = linkRepositoryPort;
	}

	public InboundNfeResolution resolve(final InboundNfe inboundNfe) {
		final Optional<SupplierId> supplierId = supplierRepositoryPort.findByDocument(inboundNfe.getSupplierDocument())
				.map(Supplier::getId);
		final Map<String, SupplierProductCatalogLink> linksByCode = supplierId
				.map(id -> linksByCode(inboundNfe, id))
				.orElse(Map.of());
		final List<InboundNfeItem> items = inboundNfe.getItems();
		final List<ItemResolution> resolutions = IntStream.range(0, items.size())
				.mapToObj(i -> new ItemResolution(i, items.get(i),
						Optional.ofNullable(linksByCode.get(items.get(i).supplierProductCode()))))
				.toList();
		return new InboundNfeResolution(supplierId, resolutions);
	}

	private Map<String, SupplierProductCatalogLink> linksByCode(final InboundNfe inboundNfe, final SupplierId supplierId) {
		final List<String> codes = inboundNfe.getItems().stream().map(InboundNfeItem::supplierProductCode).distinct().toList();
		return linkRepositoryPort.findBySupplierAndCodes(inboundNfe.getCompanyId(), supplierId, codes).stream()
				.collect(Collectors.toMap(SupplierProductCatalogLink::getSupplierProductCode, Function.identity(),
						(first, second) -> first));
	}
}
