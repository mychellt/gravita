package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkCommand;
import br.gravita.core.ports.inbound.masterdata.UpsertSupplierProductCatalogLinkUseCase;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;

@UseCase
public class UpsertSupplierProductCatalogLinkService implements UpsertSupplierProductCatalogLinkUseCase {

	private final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	public UpsertSupplierProductCatalogLinkService(final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort) {
		this.linkRepositoryPort = linkRepositoryPort;
	}

	@Override
	public SupplierProductCatalogLink execute(final UpsertSupplierProductCatalogLinkCommand command) {
		final CompanyId companyId = CompanyId.of(command.companyId());
		final SupplierId supplierId = SupplierId.of(command.supplierId());

		final SupplierProductCatalogLink link = linkRepositoryPort
				.findByCompanySupplierAndCode(companyId, supplierId, command.supplierProductCode())
				.map(existing -> existing.withLastPurchase(command.unitValue(), command.purchasedAt()))
				.orElseGet(() -> SupplierProductCatalogLink.create(companyId, supplierId, command.supplierProductCode(),
						command.productId(), command.unitValue(), command.purchasedAt()));

		return linkRepositoryPort.save(link);
	}
}
