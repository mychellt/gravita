package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkCommand;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkUseCase;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemAsExistingProductCommand;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemAsExistingProductUseCase;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;

/** GRA-246 BR5: links an unresolved item to an existing catalog product, preventing a duplicate product for it. */
@UseCase
public class ResolveInboundNfeItemAsExistingProductService implements ResolveInboundNfeItemAsExistingProductUseCase {

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final SupplierRepositoryPort supplierRepositoryPort;
	private final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;
	private final UpsertSupplierProductCatalogLinkUseCase upsertSupplierProductCatalogLinkUseCase;

	public ResolveInboundNfeItemAsExistingProductService(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final SupplierRepositoryPort supplierRepositoryPort, final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort,
			final ProductRepositoryPort productRepositoryPort,
			final UpsertSupplierProductCatalogLinkUseCase upsertSupplierProductCatalogLinkUseCase) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.supplierRepositoryPort = supplierRepositoryPort;
		this.linkRepositoryPort = linkRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
		this.upsertSupplierProductCatalogLinkUseCase = upsertSupplierProductCatalogLinkUseCase;
	}

	@Override
	public SupplierProductCatalogLink execute(final ResolveInboundNfeItemAsExistingProductCommand command) {
		final InboundNfe inboundNfe = inboundNfeRepositoryPort.findById(InboundNfeId.of(command.inboundNfeId()))
				.orElseThrow(() -> new InboundNfeNotFoundException(command.inboundNfeId()));
		final Supplier supplier = InboundNfeItemResolutionSupport.requireResolvedSupplier(supplierRepositoryPort, inboundNfe);
		final InboundNfeItem item = InboundNfeItemResolutionSupport.requireItemAt(inboundNfe, command.itemIndex());
		InboundNfeItemResolutionSupport.requireNotAlreadyLinked(linkRepositoryPort, inboundNfe.getCompanyId(), supplier.getId(), item);

		final ProductDomain product = productRepositoryPort.get(command.productId())
				.orElseThrow(() -> new BusinessRuleException("Product not found: " + command.productId()));

		return upsertSupplierProductCatalogLinkUseCase.execute(new UpsertSupplierProductCatalogLinkCommand(
				inboundNfe.getCompanyId().value(), supplier.getId().value(), item.supplierProductCode(), product.getId(),
				item.unitValue(), inboundNfe.getIssuedAt()));
	}
}
