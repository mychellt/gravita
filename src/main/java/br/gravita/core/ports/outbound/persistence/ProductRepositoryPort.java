package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.ports.outbound.persistence.commons.BaseRepositoryPort;

public interface ProductRepositoryPort extends BaseRepositoryPort<ProductDomain> {
	boolean existsByBarcode(final String barcode);
}
