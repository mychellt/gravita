package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.SupplierProductCatalogLinkJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.SupplierProductCatalogLinkPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.SupplierProductCatalogLinkJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

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
	@Transactional
	public SupplierProductCatalogLink save(final SupplierProductCatalogLink link) {
		final SupplierProductCatalogLinkJpaEntity entity = mapper.map(link);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final SupplierProductCatalogLinkJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<SupplierProductCatalogLink> findByCompanySupplierAndCode(final CompanyId companyId, final SupplierId supplierId,
			final String supplierProductCode) {
		return jpaRepository
				.findByCompanyIdAndSupplierIdAndSupplierProductCode(companyId.value(), supplierId.value(), supplierProductCode)
				.map(mapper::map);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SupplierProductCatalogLink> findByCompanyAndProduct(final CompanyId companyId, final UUID productId) {
		return jpaRepository.findByCompanyIdAndProductId(companyId.value(), productId).stream().map(mapper::map).toList();
	}
}
