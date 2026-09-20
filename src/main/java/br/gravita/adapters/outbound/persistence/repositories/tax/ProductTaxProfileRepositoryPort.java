package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.core.domain.tax.ProductTaxProfile;

import java.util.Optional;

/**
 * Resolves a product's tax profile (NCM) so the rate table can be queried.
 * Not named in the module spec's outbound-ports table; inferred here from
 * UC-M2-02's precondition on the product's M1 tax profile (M1-11). Until
 * that lands this port has no production adapter — see the PR notes on
 * GRA-32.
 */
public interface ProductTaxProfileRepositoryPort {

	Optional<ProductTaxProfile> findByProductRef(String productRef);
}
