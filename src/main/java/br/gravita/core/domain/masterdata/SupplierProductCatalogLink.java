package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Remembers which catalog {@code Product} a supplier's own item code refers to, scoped per company: the same
 * (supplier, supplierProductCode) pair resolves to the same product on every later NFe without the operator
 * re-picking it, and two suppliers can map different codes to the very same product without creating duplicates.
 * Immutable like {@code InboundNfe} - {@link #withLastPurchase} returns a new instance rather than mutating.
 */
@Getter
public final class SupplierProductCatalogLink {

	private final SupplierProductCatalogLinkId id;
	private final CompanyId companyId;
	private final SupplierId supplierId;
	private final String supplierProductCode;
	private final UUID productId;
	private final BigDecimal lastPurchaseUnitValue;
	private final Instant lastPurchaseAt;

	public SupplierProductCatalogLink(final SupplierProductCatalogLinkId id, final CompanyId companyId, final SupplierId supplierId,
			final String supplierProductCode, final UUID productId, final BigDecimal lastPurchaseUnitValue, final Instant lastPurchaseAt) {
		this.id = Objects.requireNonNull(id, "SupplierProductCatalogLinkId is required");
		this.companyId = Objects.requireNonNull(companyId, "companyId is required");
		this.supplierId = Objects.requireNonNull(supplierId, "supplierId is required");
		this.supplierProductCode = requireCode(supplierProductCode);
		this.productId = Objects.requireNonNull(productId, "productId is required");
		this.lastPurchaseUnitValue = requireNonNegative(lastPurchaseUnitValue);
		this.lastPurchaseAt = Objects.requireNonNull(lastPurchaseAt, "lastPurchaseAt is required");
	}

	public static SupplierProductCatalogLink create(final CompanyId companyId, final SupplierId supplierId, final String supplierProductCode,
			final UUID productId, final BigDecimal lastPurchaseUnitValue, final Instant lastPurchaseAt) {
		return new SupplierProductCatalogLink(SupplierProductCatalogLinkId.of(UUID.randomUUID()), companyId, supplierId,
				supplierProductCode, productId, lastPurchaseUnitValue, lastPurchaseAt);
	}

	/** Records this NFe's price/date as the last purchase - never an average of past purchases. */
	public SupplierProductCatalogLink withLastPurchase(final BigDecimal unitValue, final Instant purchasedAt) {
		return new SupplierProductCatalogLink(id, companyId, supplierId, supplierProductCode, productId, unitValue, purchasedAt);
	}

	public boolean matchesKey(final CompanyId companyId, final SupplierId supplierId, final String supplierProductCode) {
		return this.companyId.equals(companyId) && this.supplierId.equals(supplierId)
				&& this.supplierProductCode.equals(supplierProductCode);
	}

	private static String requireCode(final String supplierProductCode) {
		if (supplierProductCode == null || supplierProductCode.isBlank()) {
			throw new BusinessRuleException("supplierProductCode is required");
		}
		return supplierProductCode;
	}

	private static BigDecimal requireNonNegative(final BigDecimal value) {
		if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("lastPurchaseUnitValue cannot be negative: " + value);
		}
		return value;
	}
}
