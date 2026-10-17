package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindSupplierProductCatalogLinkUseCase {

	Optional<SupplierProductCatalogLink> bySupplierProductCode(CompanyId companyId, SupplierId supplierId,
			String supplierProductCode);

	/** Every supplier link of the product within the company, each with its own last purchase price/date. */
	List<SupplierProductCatalogLink> byProduct(CompanyId companyId, UUID productId);
}
