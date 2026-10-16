package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.ports.inbound.masterdata.QuerySupplierProductCatalogLinkUseCase;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@UseCase
public class QuerySupplierProductCatalogLinkService implements QuerySupplierProductCatalogLinkUseCase {

	private final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	public QuerySupplierProductCatalogLinkService(final SupplierProductCatalogLinkRepositoryPort linkRepositoryPort) {
		this.linkRepositoryPort = linkRepositoryPort;
	}

	@Override
	public Optional<SupplierProductCatalogLink> findByCode(final UUID companyId, final UUID supplierId, final String supplierProductCode) {
		return linkRepositoryPort.findByCompanySupplierAndCode(CompanyId.of(companyId), SupplierId.of(supplierId), supplierProductCode);
	}

	@Override
	public List<SupplierProductCatalogLink> findByProduct(final UUID companyId, final UUID productId) {
		return linkRepositoryPort.findByCompanyAndProduct(CompanyId.of(companyId), productId);
	}
}
