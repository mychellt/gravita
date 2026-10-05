package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.ProductJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.ProductJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.ProductTaxProfile;
import br.gravita.core.ports.outbound.persistence.tax.ProductTaxProfileRepositoryPort;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class ProductTaxProfileRepositoryAdapter implements ProductTaxProfileRepositoryPort {

	private final ProductJpaRepository jpaRepository;

	ProductTaxProfileRepositoryAdapter(final ProductJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<ProductTaxProfile> findByProductRef(final String productRef) {
		final UUID productId;
		try {
			productId = UUID.fromString(productRef);
		} catch (final IllegalArgumentException notAUuid) {
			return Optional.empty();
		}
		return jpaRepository.findById(productId)
				.map(ProductJpaEntity::getNcm)
				.filter(ncm -> ncm != null && !ncm.isBlank())
				.map(ncm -> new ProductTaxProfile(productRef, ncm));
	}
}
