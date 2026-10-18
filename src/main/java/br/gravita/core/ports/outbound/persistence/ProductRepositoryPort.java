package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.ports.outbound.persistence.commons.BaseRepositoryPort;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort extends BaseRepositoryPort<ProductDomain> {
	boolean existsByBarcode(final String barcode);

	/** The product registered under the given barcode; barcodes are unique across the catalog. */
	Optional<ProductDomain> findByBarcode(final String barcode);

	/** The products whose NCM has the given digits, however it was typed ("6907.22.00" and "69072200" are the same). */
	List<ProductDomain> findAllByNcm(final String ncmDigits);

	/** The products with the given ids; unknown ids are simply absent from the result. */
	List<ProductDomain> findAllByIds(Collection<UUID> ids);
}
