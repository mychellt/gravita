package br.gravita.core.ports.persistence;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.ports.persistence.commons.BaseRepositoryPort;

public interface ProductRepositoryPort extends BaseRepositoryPort<ProductDomain> {
	boolean existsByBarcode(final String barcode);
}
