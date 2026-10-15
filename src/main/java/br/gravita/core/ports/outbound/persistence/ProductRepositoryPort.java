package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.ports.outbound.persistence.commons.BaseRepositoryPort;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductRepositoryPort extends BaseRepositoryPort<ProductDomain> {
	boolean existsByBarcode(final String barcode);

	/** The products with the given ids; unknown ids are simply absent from the result. */
	List<ProductDomain> findAllByIds(Collection<UUID> ids);
}
