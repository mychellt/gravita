package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierProductCatalogLinkRepositoryPort {

	SupplierProductCatalogLink save(SupplierProductCatalogLink link);

	Optional<SupplierProductCatalogLink> findByCompanySupplierAndCode(CompanyId companyId, SupplierId supplierId,
			String supplierProductCode);

	/** Every supplier currently linked to this product within the company, for a future per-product supplier listing. */
	List<SupplierProductCatalogLink> findByCompanyAndProduct(CompanyId companyId, UUID productId);
}
