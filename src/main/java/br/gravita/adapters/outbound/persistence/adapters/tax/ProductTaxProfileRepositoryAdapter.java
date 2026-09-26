package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.ProductJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.ProductJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.ProductTaxProfile;
import br.gravita.core.ports.outbound.persistence.tax.ProductTaxProfileRepositoryPort;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code productRef} is the product's own UUID, stringified - the same
 * identity every M3 caller already uses ({@link br.gravita.core.domain.tax.SaleItem#productId()}).
 * GRA-32's previously-unwired production adapter (no NCM catalog existed
 * anywhere else); the NCM itself is a plain field on the product record.
 */
@PersistenceAdapter
class ProductTaxProfileRepositoryAdapter implements ProductTaxProfileRepositoryPort {

	private final ProductJpaRepository jpaRepository;

	ProductTaxProfileRepositoryAdapter(ProductJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<ProductTaxProfile> findByProductRef(String productRef) {
		UUID productId;
		try {
			productId = UUID.fromString(productRef);
		} catch (IllegalArgumentException notAUuid) {
			return Optional.empty();
		}
		return jpaRepository.findById(productId)
				.map(ProductJpaEntity::getNcm)
				.filter(ncm -> ncm != null && !ncm.isBlank())
				.map(ncm -> new ProductTaxProfile(productRef, ncm));
	}
}
