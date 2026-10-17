package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Every lookup is scoped by company: a link of one company is never visible to another. */
public interface SupplierProductCatalogLinkRepositoryPort {
	SupplierProductCatalogLink save(SupplierProductCatalogLink link);

	Optional<SupplierProductCatalogLink> findBySupplierAndCode(CompanyId companyId, SupplierId supplierId,
			String supplierProductCode);

	/** The links that exist for the given codes; codes without a link are simply absent from the result. */
	List<SupplierProductCatalogLink> findBySupplierAndCodes(CompanyId companyId, SupplierId supplierId,
			Collection<String> supplierProductCodes);

	List<SupplierProductCatalogLink> findAllByProduct(CompanyId companyId, UUID productId);
}
