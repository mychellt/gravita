package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;

public interface ResolveInboundNfeItemAsExistingProductUseCase {
	SupplierProductCatalogLink execute(ResolveInboundNfeItemAsExistingProductCommand command);
}
