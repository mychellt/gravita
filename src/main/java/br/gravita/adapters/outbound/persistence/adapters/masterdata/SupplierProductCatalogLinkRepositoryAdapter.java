package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierProductCatalogLinkJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.SupplierProductCatalogLinkPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.SupplierProductCatalogLinkJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class SupplierProductCatalogLinkRepositoryAdapter implements SupplierProductCatalogLinkRepositoryPort {

	private final SupplierProductCatalogLinkJpaRepository jpaRepository;
	private final SupplierProductCatalogLinkPersistenceMapper mapper;

	SupplierProductCatalogLinkRepositoryAdapter(final SupplierProductCatalogLinkJpaRepository jpaRepository,
			final SupplierProductCatalogLinkPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public SupplierProductCatalogLink save(final SupplierProductCatalogLink link) {
		final SupplierProductCatalogLinkJpaEntity entity = mapper.map(link);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.map(jpaRepository.saveAndFlush(entity));
	}

	@Override
	public Optional<SupplierProductCatalogLink> findBySupplierAndCode(final CompanyId companyId, final SupplierId supplierId,
			final String supplierProductCode) {
		return jpaRepository
				.findByCompanyIdAndSupplierIdAndSupplierProductCode(companyId.value(), supplierId.value(), supplierProductCode)
				.map(mapper::map);
	}

	@Override
	public List<SupplierProductCatalogLink> findBySupplierAndCodes(final CompanyId companyId, final SupplierId supplierId,
			final Collection<String> supplierProductCodes) {
		if (supplierProductCodes.isEmpty()) {
			return List.of();
		}
		return jpaRepository
				.findByCompanyIdAndSupplierIdAndSupplierProductCodeIn(companyId.value(), supplierId.value(),
						supplierProductCodes)
				.stream().map(mapper::map).toList();
	}

	@Override
	public List<SupplierProductCatalogLink> findAllByProduct(final CompanyId companyId, final UUID productId) {
		return jpaRepository.findByCompanyIdAndProductId(companyId.value(), productId).stream().map(mapper::map).toList();
	}
}
