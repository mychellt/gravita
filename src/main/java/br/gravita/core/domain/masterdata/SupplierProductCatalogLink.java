package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Remembers which catalog {@code Product} a supplier's own product code stands for, within one company, together with
 * the last price paid for it. Unique per (company, supplier, supplier product code); a Product may be linked from
 * several suppliers at once.
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
			final String supplierProductCode, final UUID productId, final BigDecimal lastPurchaseUnitValue,
			final Instant lastPurchaseAt) {
		this.id = Objects.requireNonNull(id, "SupplierProductCatalogLinkId is required");
		this.companyId = Objects.requireNonNull(companyId, "companyId is required");
		this.supplierId = Objects.requireNonNull(supplierId, "supplierId is required");
		this.supplierProductCode = requireCode(supplierProductCode);
		this.productId = Objects.requireNonNull(productId, "productId is required");
		this.lastPurchaseUnitValue = requireNonNegative(lastPurchaseUnitValue);
		this.lastPurchaseAt = Objects.requireNonNull(lastPurchaseAt, "lastPurchaseAt is required");
	}

	public static SupplierProductCatalogLink create(final CompanyId companyId, final SupplierId supplierId,
			final String supplierProductCode, final UUID productId, final BigDecimal unitValue, final Instant purchasedAt) {
		return new SupplierProductCatalogLink(SupplierProductCatalogLinkId.random(), companyId, supplierId,
				supplierProductCode, productId, unitValue, purchasedAt);
	}

	/** The same link carrying the price and date of the latest purchase: the last price, never an average. */
	public SupplierProductCatalogLink withLastPurchase(final BigDecimal unitValue, final Instant purchasedAt) {
		return new SupplierProductCatalogLink(id, companyId, supplierId, supplierProductCode, productId, unitValue,
				purchasedAt);
	}

	/** {@code unitValue} minus the last price paid; positive when the item became more expensive. */
	public BigDecimal priceDelta(final BigDecimal unitValue) {
		return unitValue.subtract(lastPurchaseUnitValue);
	}

	public boolean hasPriceVariance(final BigDecimal unitValue) {
		return priceDelta(unitValue).signum() != 0;
	}

	private static String requireCode(final String supplierProductCode) {
		if (supplierProductCode == null || supplierProductCode.isBlank()) {
			throw new BusinessRuleException("supplierProductCode is required");
		}
		return supplierProductCode;
	}

	private static BigDecimal requireNonNegative(final BigDecimal lastPurchaseUnitValue) {
		Objects.requireNonNull(lastPurchaseUnitValue, "lastPurchaseUnitValue is required");
		if (lastPurchaseUnitValue.signum() < 0) {
			throw new BusinessRuleException("lastPurchaseUnitValue cannot be negative: " + lastPurchaseUnitValue);
		}
		return lastPurchaseUnitValue;
	}
}
