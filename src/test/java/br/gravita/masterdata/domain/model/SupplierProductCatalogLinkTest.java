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

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
	private final UUID productId = UUID.randomUUID();
	private final Instant purchasedAt = Instant.parse("2026-01-10T12:00:00Z");

	@Test
	@DisplayName("Creates a link with a freshly assigned id")
	void shouldCreateLinkWithFreshId() {
		final SupplierProductCatalogLink link = SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001",
				productId, new BigDecimal("10.00"), purchasedAt);

		assertThat(link.getId()).isNotNull();
		assertThat(link.getCompanyId()).isEqualTo(companyId);
		assertThat(link.getSupplierId()).isEqualTo(supplierId);
		assertThat(link.getSupplierProductCode()).isEqualTo("SKU-001");
		assertThat(link.getProductId()).isEqualTo(productId);
		assertThat(link.getLastPurchaseUnitValue()).isEqualByComparingTo("10.00");
		assertThat(link.getLastPurchaseAt()).isEqualTo(purchasedAt);
	}

	@Test
	@DisplayName("BR7: withLastPurchase replaces the price and date with this NFe's own values, never an average")
	void withLastPurchaseReplacesPriceAndDate() {
		final SupplierProductCatalogLink original = SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001",
				productId, new BigDecimal("10.00"), purchasedAt);
		final Instant laterPurchase = purchasedAt.plusSeconds(86_400);

		final SupplierProductCatalogLink updated = original.withLastPurchase(new BigDecimal("12.50"), laterPurchase);

		assertThat(updated.getId()).isEqualTo(original.getId());
		assertThat(updated.getProductId()).isEqualTo(original.getProductId());
		assertThat(updated.getLastPurchaseUnitValue()).isEqualByComparingTo("12.50");
		assertThat(updated.getLastPurchaseAt()).isEqualTo(laterPurchase);
	}

	@Test
	@DisplayName("Rejects a blank supplierProductCode")
	void rejectsBlankSupplierProductCode() {
		assertThatThrownBy(() -> SupplierProductCatalogLink.create(companyId, supplierId, "  ", productId,
				BigDecimal.TEN, purchasedAt)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a negative lastPurchaseUnitValue")
	void rejectsNegativeLastPurchaseUnitValue() {
		assertThatThrownBy(() -> SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001", productId,
				new BigDecimal("-1"), purchasedAt)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("matchesKey only matches on the exact (company, supplier, code) triple")
	void matchesKeyOnlyOnExactTriple() {
		final SupplierProductCatalogLink link = SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001",
				productId, BigDecimal.TEN, purchasedAt);

		assertThat(link.matchesKey(companyId, supplierId, "SKU-001")).isTrue();
		assertThat(link.matchesKey(CompanyId.of(UUID.randomUUID()), supplierId, "SKU-001")).isFalse();
		assertThat(link.matchesKey(companyId, SupplierId.of(UUID.randomUUID()), "SKU-001")).isFalse();
		assertThat(link.matchesKey(companyId, supplierId, "OTHER")).isFalse();
	}
}
