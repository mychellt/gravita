package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;

/**
 * Creates the link for (company, supplier, supplier product code), or refreshes the last purchase price/date of the
 * existing one. An existing link is never re-pointed to another product.
 */
public interface UpsertSupplierProductCatalogLinkUseCase {
	SupplierProductCatalogLink execute(UpsertSupplierProductCatalogLinkCommand command);
}
