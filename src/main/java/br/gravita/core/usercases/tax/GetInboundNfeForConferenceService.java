package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.ports.inbound.tax.GetInboundNfeForConferenceUseCase;
import br.gravita.core.ports.inbound.tax.InboundNfeConferenceView;
import br.gravita.core.ports.inbound.tax.InboundNfeConferenceView.InboundNfeConferenceItemView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import java.util.Optional;
import java.util.UUID;

@UseCase
public class GetInboundNfeForConferenceService implements GetInboundNfeForConferenceUseCase {

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final SupplierRepositoryPort supplierRepositoryPort;
	private final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	public GetInboundNfeForConferenceService(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final SupplierRepositoryPort supplierRepositoryPort, final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort,
			final ProductRepositoryPort productRepositoryPort) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.supplierRepositoryPort = supplierRepositoryPort;
		this.linkRepositoryPort = linkRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public InboundNfeConferenceView execute(final UUID inboundNfeId) {
		final InboundNfe inboundNfe = inboundNfeRepositoryPort.findById(InboundNfeId.of(inboundNfeId))
				.orElseThrow(() -> new InboundNfeNotFoundException(inboundNfeId));

		final Optional<Supplier> supplier = supplierRepositoryPort.findByDocument(inboundNfe.getSupplierDocument());

		final var items = inboundNfe.getItems().stream()
				.map(item -> toItemView(inboundNfe.getCompanyId(), supplier, item))
				.toList();

		return new InboundNfeConferenceView(inboundNfe.getId().value(), inboundNfe.getSeries(), inboundNfe.getNumber(),
				inboundNfe.getSupplierName(), supplier.isPresent(), supplier.map(s -> s.getId().value()).orElse(null), items);
	}

	private InboundNfeConferenceItemView toItemView(final CompanyId companyId, final Optional<Supplier> supplier,
			final InboundNfeItem item) {
		final Optional<SupplierProductCatalogLink> link = supplier
				.flatMap(s -> linkRepositoryPort.findByCompanySupplierAndCode(companyId, s.getId(), item.supplierProductCode()));
		final Optional<ProductDomain> product = link.flatMap(l -> productRepositoryPort.get(l.getProductId()));
		return new InboundNfeConferenceItemView(item.supplierProductCode(), item.description(), item.ncm(), item.unit(),
				item.quantity(), item.unitValue(), link.map(SupplierProductCatalogLink::getProductId).orElse(null),
				product.map(ProductDomain::getInternalCode).orElse(null),
				link.map(SupplierProductCatalogLink::getLastPurchaseUnitValue).orElse(null));
	}
}
