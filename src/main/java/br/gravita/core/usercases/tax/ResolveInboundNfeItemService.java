package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeResolution;
import br.gravita.core.ports.business.RegisterProductPort;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkCommand;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkUseCase;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemCommand;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemUseCase;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class ResolveInboundNfeItemService implements ResolveInboundNfeItemUseCase {

	private final OwnedInboundNfeLoader ownedInboundNfeLoader;
	private final InboundNfeResolver inboundNfeResolver;
	private final RegisterProductPort registerProductPort;
	private final UpsertSupplierProductCatalogLinkUseCase upsertLinkUseCase;

	public ResolveInboundNfeItemService(final OwnedInboundNfeLoader ownedInboundNfeLoader,
			final InboundNfeResolver inboundNfeResolver, final RegisterProductPort registerProductPort,
			final UpsertSupplierProductCatalogLinkUseCase upsertLinkUseCase) {
		this.ownedInboundNfeLoader = ownedInboundNfeLoader;
		this.inboundNfeResolver = inboundNfeResolver;
		this.registerProductPort = registerProductPort;
		this.upsertLinkUseCase = upsertLinkUseCase;
	}

	/** One transaction, so a newly registered Product never exists without the link the operator asked for. */
	@Override
	@Transactional
	public SupplierProductCatalogLink execute(final ResolveInboundNfeItemCommand command) {
		final InboundNfe inboundNfe = ownedInboundNfeLoader.load(command.inboundNfeId(), command.callerId());
		final InboundNfeResolution resolution = inboundNfeResolver.resolve(inboundNfe);
		final SupplierId supplierId = resolution.supplierId().orElseThrow(() -> new BusinessRuleException(
				"No registered supplier matches the CNPJ of this NF-e; register the supplier before resolving its items"));
		final InboundNfeItem item = requireUnresolvedItem(inboundNfe, resolution, command.itemIndex());

		final ProductDomain product = command.newProduct() == null ? null : registerProductPort.execute(new Context(command.newProduct()));
		final UUID productId = product == null ? command.existingProductId() : product.getId();
		return upsertLinkUseCase.execute(new UpsertSupplierProductCatalogLinkCommand(inboundNfe.getCompanyId(), supplierId,
				item.supplierProductCode(), productId, item.unitValue(), inboundNfe.getIssuedAt()));
	}

	private InboundNfeItem requireUnresolvedItem(final InboundNfe inboundNfe, final InboundNfeResolution resolution,
			final int itemIndex) {
		if (itemIndex < 0 || itemIndex >= inboundNfe.getItems().size()) {
			throw new BusinessRuleException("The NF-e has no item at position " + (itemIndex + 1));
		}
		if (resolution.items().get(itemIndex).isResolved()) {
			throw new BusinessRuleException("Item " + (itemIndex + 1) + " is already linked to a product");
		}
		return inboundNfe.getItems().get(itemIndex);
	}
}
