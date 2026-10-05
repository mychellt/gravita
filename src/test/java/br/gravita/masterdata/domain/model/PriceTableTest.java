package br.gravita.masterdata.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.masterdata.*;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PriceTableTest {

	private static final ProductOrClassRef PRODUCT_REF = ProductOrClassRef.product(UUID.randomUUID().toString());

	@Test
	@DisplayName("Rejects a validTo date before validFrom")
	void shouldRejectValidToBeforeValidFrom() {
		assertThatThrownBy(() -> table(PriceFormation.FIXED, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 1), null,
				null, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("validTo");
	}

	@Test
	@DisplayName("Considers the table active within its validity window")
	void shouldConsiderTableActiveWithinItsValidityWindow() {
		final PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThat(table.isActive(LocalDate.of(2026, 6, 1))).isTrue();
	}

	@Test
	@DisplayName("Automatically excludes the table once validTo is in the past")
	void shouldExcludeTableAutomaticallyOnceValidToIsInThePast() {
		final PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThat(table.isActive(LocalDate.of(2026, 1, 1))).isFalse();
	}

	@Test
	@DisplayName("Considers the table active indefinitely when validTo is absent")
	void shouldConsiderTableActiveIndefinitelyWhenValidToIsAbsent() {
		final PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2020, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThat(table.isActive(LocalDate.of(2099, 1, 1))).isTrue();
	}

	@Test
	@DisplayName("Resolves a fixed price directly from the entry value")
	void shouldResolveFixedPriceDirectlyFromEntryValue() {
		final PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, new BigDecimal("99.90"))));

		final BigDecimal resolved = table.resolvePrice(PRODUCT_REF, new BigDecimal("50.00"), new BigDecimal("70.00"));

		assertThat(resolved).isEqualByComparingTo("99.90");
	}

	@Test
	@DisplayName("Resolves a percent-over-cost price from the product's current cost at resolution time")
	void shouldResolvePercentOverCostFromCurrentProductCostAtResolutionTime() {
		final PriceTable table = table(PriceFormation.PERCENT_OVER_COST, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.valueOf(20))));

		final BigDecimal resolvedBefore = table.resolvePrice(PRODUCT_REF, new BigDecimal("100.00"), null);
		final BigDecimal resolvedAfterCostIncrease = table.resolvePrice(PRODUCT_REF, new BigDecimal("200.00"), null);

		assertThat(resolvedBefore).isEqualByComparingTo("120.00");
		assertThat(resolvedAfterCostIncrease).isEqualByComparingTo("240.00");
	}

	@Test
	@DisplayName("Resolves a percent-over-base price from the product's current base price")
	void shouldResolvePercentOverBaseFromCurrentProductBasePrice() {
		final PriceTable table = table(PriceFormation.PERCENT_OVER_BASE, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.valueOf(-10))));

		final BigDecimal resolved = table.resolvePrice(PRODUCT_REF, null, new BigDecimal("100.00"));

		assertThat(resolved).isEqualByComparingTo("90.00");
	}

	@Test
	@DisplayName("Rejects a fixed entry with a non-positive value")
	void shouldRejectFixedEntryWithNonPositiveValue() {
		assertThatThrownBy(() -> table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.ZERO))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("FIXED");
	}

	@Test
	@DisplayName("Allows a discount below the maximum when the behavior is BLOCK")
	void shouldAllowDiscountBelowMaxWithBlockBehavior() {
		final PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, BigDecimal.valueOf(10),
				MaxDiscountBehavior.BLOCK, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThat(table.evaluateDiscount(BigDecimal.valueOf(5))).isEqualTo(DiscountCheckResult.ALLOWED);
	}

	@Test
	@DisplayName("Blocks a discount exceeding the maximum when the behavior is BLOCK")
	void shouldBlockDiscountExceedingMaxWhenBehaviorIsBlock() {
		final PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, BigDecimal.valueOf(10),
				MaxDiscountBehavior.BLOCK, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThatThrownBy(() -> table.evaluateDiscount(BigDecimal.valueOf(15)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("exceeds");
	}

	@Test
	@DisplayName("Alerts without blocking when the behavior is ALERT")
	void shouldAlertWithoutBlockingWhenBehaviorIsAlert() {
		final PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, BigDecimal.valueOf(10),
				MaxDiscountBehavior.ALERT, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThatCode(() -> assertThat(table.evaluateDiscount(BigDecimal.valueOf(15))).isEqualTo(DiscountCheckResult.ALERT))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Rejects a maximum discount percent without a behavior")
	void shouldRejectMaxDiscountPercentWithoutBehavior() {
		assertThatThrownBy(() -> table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, BigDecimal.TEN, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("maxDiscountPercent");
	}

	@Test
	@DisplayName("Rejects duplicate entry references")
	void shouldRejectDuplicateEntryReferences() {
		assertThatThrownBy(() -> table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN), new PriceTableEntry(PRODUCT_REF, BigDecimal.ONE))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Duplicate");
	}

	@Test
	@DisplayName("Allows unlimited entries and tables, with no upper bound enforced")
	void shouldAllowUnlimitedEntriesAndTablesWithNoUpperBoundEnforced() {
		assertThatCode(() -> table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN))))
				.doesNotThrowAnyException();
	}

	private PriceTable table(final PriceFormation formation, final LocalDate validFrom, final LocalDate validTo,
			final BigDecimal maxDiscountPercent, final MaxDiscountBehavior maxDiscountBehavior, final List<PriceTableEntry> entries) {
		return PriceTable.of(PriceTableId.of(UUID.randomUUID()), formation, validFrom, validTo, maxDiscountPercent,
				maxDiscountBehavior, entries);
	}
}
