package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UpsertSupplierProductCatalogLinkCommand(CompanyId companyId, SupplierId supplierId, String supplierProductCode,
		UUID productId, BigDecimal unitValue, Instant purchasedAt) {

	public UpsertSupplierProductCatalogLinkCommand {
		Objects.requireNonNull(companyId, "companyId is required");
		Objects.requireNonNull(supplierId, "supplierId is required");
		Objects.requireNonNull(supplierProductCode, "supplierProductCode is required");
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(unitValue, "unitValue is required");
		Objects.requireNonNull(purchasedAt, "purchasedAt is required");
	}
}
