package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.ports.business.RegisterProductPort;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkCommand;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkUseCase;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemAsNewProductCommand;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemAsNewProductUseCase;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;

/** GRA-246 BR4: registers a brand-new catalog product from an unresolved item, then links it so the next purchase auto-resolves. */
@UseCase
public class ResolveInboundNfeItemAsNewProductService implements ResolveInboundNfeItemAsNewProductUseCase {

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final SupplierRepositoryPort supplierRepositoryPort;
	private final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;
	private final RegisterProductPort registerProductPort;
	private final UpsertSupplierProductCatalogLinkUseCase upsertSupplierProductCatalogLinkUseCase;

	public ResolveInboundNfeItemAsNewProductService(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final SupplierRepositoryPort supplierRepositoryPort, final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort,
			final RegisterProductPort registerProductPort,
			final UpsertSupplierProductCatalogLinkUseCase upsertSupplierProductCatalogLinkUseCase) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.supplierRepositoryPort = supplierRepositoryPort;
		this.linkRepositoryPort = linkRepositoryPort;
		this.registerProductPort = registerProductPort;
		this.upsertSupplierProductCatalogLinkUseCase = upsertSupplierProductCatalogLinkUseCase;
	}

	@Override
	public ProductDomain execute(final ResolveInboundNfeItemAsNewProductCommand command) {
		final InboundNfe inboundNfe = inboundNfeRepositoryPort.findById(InboundNfeId.of(command.inboundNfeId()))
				.orElseThrow(() -> new InboundNfeNotFoundException(command.inboundNfeId()));
		final Supplier supplier = InboundNfeItemResolutionSupport.requireResolvedSupplier(supplierRepositoryPort, inboundNfe);
		final InboundNfeItem item = InboundNfeItemResolutionSupport.requireItemAt(inboundNfe, command.itemIndex());
		InboundNfeItemResolutionSupport.requireNotAlreadyLinked(linkRepositoryPort, inboundNfe.getCompanyId(), supplier.getId(), item);

		final ProductDomain created = registerProductPort.execute(new Context(command.productToRegister()));

		upsertSupplierProductCatalogLinkUseCase.execute(new UpsertSupplierProductCatalogLinkCommand(
				inboundNfe.getCompanyId().value(), supplier.getId().value(), item.supplierProductCode(), created.getId(),
				item.unitValue(), inboundNfe.getIssuedAt()));

		return created;
	}
}
