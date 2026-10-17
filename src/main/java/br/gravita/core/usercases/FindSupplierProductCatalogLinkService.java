package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.ports.inbound.masterdata.FindSupplierProductCatalogLinkUseCase;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@UseCase
public class FindSupplierProductCatalogLinkService implements FindSupplierProductCatalogLinkUseCase {

	private final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	public FindSupplierProductCatalogLinkService(final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort) {
		this.linkRepositoryPort = linkRepositoryPort;
	}

	@Override
	public Optional<SupplierProductCatalogLink> bySupplierProductCode(final CompanyId companyId, final SupplierId supplierId,
			final String supplierProductCode) {
		return linkRepositoryPort.findBySupplierAndCode(companyId, supplierId, supplierProductCode);
	}

	@Override
	public List<SupplierProductCatalogLink> byProduct(final CompanyId companyId, final UUID productId) {
		return linkRepositoryPort.findAllByProduct(companyId, productId);
	}
}
