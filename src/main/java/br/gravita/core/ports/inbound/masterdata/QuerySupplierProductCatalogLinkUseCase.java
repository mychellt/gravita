package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuerySupplierProductCatalogLinkUseCase {

	Optional<SupplierProductCatalogLink> findByCode(UUID companyId, UUID supplierId, String supplierProductCode);

	/** Every supplier linked to this product within the company - for a future per-product supplier listing. */
	List<SupplierProductCatalogLink> findByProduct(UUID companyId, UUID productId);
}
