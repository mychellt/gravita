package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.ProductTaxProfile;

import java.util.Optional;

public interface ProductTaxProfileRepositoryPort {

	Optional<ProductTaxProfile> findByProductRef(String productRef);
}
