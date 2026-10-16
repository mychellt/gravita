package br.gravita.core.domain.masterdata;

import java.util.Objects;
import java.util.UUID;

public record SupplierProductCatalogLinkId(UUID value) {

	public SupplierProductCatalogLinkId {
		Objects.requireNonNull(value, "SupplierProductCatalogLinkId value is required");
	}

	public static SupplierProductCatalogLinkId of(final UUID value) {
		return new SupplierProductCatalogLinkId(value);
	}
}
