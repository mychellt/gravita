package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;

public interface UpsertSupplierProductCatalogLinkUseCase {
	SupplierProductCatalogLink execute(UpsertSupplierProductCatalogLinkCommand command);
}
