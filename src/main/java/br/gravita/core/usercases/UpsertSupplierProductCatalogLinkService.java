package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.SupplierNotFoundException;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkCommand;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkUseCase;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;

@UseCase
public class UpsertSupplierProductCatalogLinkService implements UpsertSupplierProductCatalogLinkUseCase {

	private final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;
	private final SupplierRepositoryPort supplierRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	public UpsertSupplierProductCatalogLinkService(final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort,
			final SupplierRepositoryPort supplierRepositoryPort, final ProductRepositoryPort productRepositoryPort) {
		this.linkRepositoryPort = linkRepositoryPort;
		this.supplierRepositoryPort = supplierRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public SupplierProductCatalogLink execute(final UpsertSupplierProductCatalogLinkCommand command) {
		supplierRepositoryPort.findById(command.supplierId())
				.orElseThrow(() -> new SupplierNotFoundException(command.supplierId().value()));
		if (productRepositoryPort.get(command.productId()).isEmpty()) {
			throw new BusinessRuleException("Product not found: " + command.productId());
		}

		final SupplierProductCatalogLink link = linkRepositoryPort
				.findBySupplierAndCode(command.companyId(), command.supplierId(), command.supplierProductCode())
				.map(existing -> refresh(existing, command))
				.orElseGet(() -> SupplierProductCatalogLink.create(command.companyId(), command.supplierId(),
						command.supplierProductCode(), command.productId(), command.unitValue(), command.purchasedAt()));
		return linkRepositoryPort.save(link);
	}

	private SupplierProductCatalogLink refresh(final SupplierProductCatalogLink existing,
			final UpsertSupplierProductCatalogLinkCommand command) {
		if (!existing.getProductId().equals(command.productId())) {
			throw new BusinessRuleException("Supplier product code " + command.supplierProductCode()
					+ " is already linked to another product");
		}
		return existing.withLastPurchase(command.unitValue(), command.purchasedAt());
	}
}
