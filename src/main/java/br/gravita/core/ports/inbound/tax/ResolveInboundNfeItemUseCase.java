package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;

/**
 * Creates the {@link SupplierProductCatalogLink} for an unresolved item of an inbound NFe, seeded with the item's unit
 * value. The imported NFe and its item snapshot are left untouched.
 */
public interface ResolveInboundNfeItemUseCase {
	SupplierProductCatalogLink execute(ResolveInboundNfeItemCommand command);
}
