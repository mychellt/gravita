package br.gravita.masterdata.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SupplierProductCatalogLinkTest {

	private static final Instant FIRST_PURCHASE = Instant.parse("2026-01-10T12:00:00Z");
	private static final Instant SECOND_PURCHASE = Instant.parse("2026-02-10T12:00:00Z");

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
	private final UUID productId = UUID.randomUUID();

	@Test
	@DisplayName("Keeps the last purchase price and date it was created with")
	void createsWithTheFirstPurchase() {
		final SupplierProductCatalogLink link = link("10.0000");

		assertThat(link.getProductId()).isEqualTo(productId);
		assertThat(link.getSupplierProductCode()).isEqualTo("SKU-001");
		assertThat(link.getLastPurchaseUnitValue()).isEqualByComparingTo("10.0000");
		assertThat(link.getLastPurchaseAt()).isEqualTo(FIRST_PURCHASE);
	}

	@Test
	@DisplayName("A new purchase replaces the last price and date, it does not average them")
	void newPurchaseReplacesTheLastPrice() {
		final SupplierProductCatalogLink link = link("10.0000");

		final SupplierProductCatalogLink updated = link.withLastPurchase(new BigDecimal("14.0000"), SECOND_PURCHASE);

		assertThat(updated.getId()).isEqualTo(link.getId());
		assertThat(updated.getProductId()).isEqualTo(productId);
		assertThat(updated.getLastPurchaseUnitValue()).isEqualByComparingTo("14.0000");
		assertThat(updated.getLastPurchaseAt()).isEqualTo(SECOND_PURCHASE);
		assertThat(link.getLastPurchaseUnitValue()).isEqualByComparingTo("10.0000");
	}

	@Test
	@DisplayName("Reports the price delta against the last purchase and whether there is any variance")
	void reportsThePriceDelta() {
		final SupplierProductCatalogLink link = link("10.0000");

		assertThat(link.priceDelta(new BigDecimal("12.5000"))).isEqualByComparingTo("2.5000");
		assertThat(link.priceDelta(new BigDecimal("9.0000"))).isEqualByComparingTo("-1.0000");
		assertThat(link.hasPriceVariance(new BigDecimal("12.5000"))).isTrue();
		assertThat(link.hasPriceVariance(new BigDecimal("10"))).isFalse();
	}

	@Test
	@DisplayName("Rejects a blank supplier product code and a negative price")
	void rejectsInvalidValues() {
		assertThatThrownBy(() -> SupplierProductCatalogLink.create(companyId, supplierId, " ", productId, BigDecimal.ONE,
				FIRST_PURCHASE)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> link("-1")).isInstanceOf(BusinessRuleException.class);
	}

	private SupplierProductCatalogLink link(final String unitValue) {
		return SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001", productId, new BigDecimal(unitValue),
				FIRST_PURCHASE);
	}
}
