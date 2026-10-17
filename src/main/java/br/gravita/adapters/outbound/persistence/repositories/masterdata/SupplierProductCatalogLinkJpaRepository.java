package br.gravita.adapters.outbound.persistence.repositories.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierProductCatalogLinkJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierProductCatalogLinkJpaRepository extends JpaRepository<SupplierProductCatalogLinkJpaEntity, UUID> {

	Optional<SupplierProductCatalogLinkJpaEntity> findByCompanyIdAndSupplierIdAndSupplierProductCode(UUID companyId,
			UUID supplierId, String supplierProductCode);

	List<SupplierProductCatalogLinkJpaEntity> findByCompanyIdAndSupplierIdAndSupplierProductCodeIn(UUID companyId,
			UUID supplierId, Collection<String> supplierProductCodes);

	List<SupplierProductCatalogLinkJpaEntity> findByCompanyIdAndProductId(UUID companyId, UUID productId);
}
