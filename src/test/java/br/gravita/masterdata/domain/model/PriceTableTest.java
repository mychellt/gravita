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
import org.junit.jupiter.api.Test;

class PriceTableTest {

	private static final ProductOrClassRef PRODUCT_REF = ProductOrClassRef.product(UUID.randomUUID().toString());

	@Test
	void shouldRejectValidToBeforeValidFrom() {
		assertThatThrownBy(() -> table(PriceFormation.FIXED, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 1), null,
				null, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("validTo");
	}

	@Test
	void shouldConsiderTableActiveWithinItsValidityWindow() {
		PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThat(table.isActive(LocalDate.of(2026, 6, 1))).isTrue();
	}

	@Test
	void shouldExcludeTableAutomaticallyOnceValidToIsInThePast() {
		PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThat(table.isActive(LocalDate.of(2026, 1, 1))).isFalse();
	}

	@Test
	void shouldConsiderTableActiveIndefinitelyWhenValidToIsAbsent() {
		PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2020, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThat(table.isActive(LocalDate.of(2099, 1, 1))).isTrue();
	}

	@Test
	void shouldResolveFixedPriceDirectlyFromEntryValue() {
		PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, new BigDecimal("99.90"))));

		BigDecimal resolved = table.resolvePrice(PRODUCT_REF, new BigDecimal("50.00"), new BigDecimal("70.00"));

		assertThat(resolved).isEqualByComparingTo("99.90");
	}

	@Test
	void shouldResolvePercentOverCostFromCurrentProductCostAtResolutionTime() {
		PriceTable table = table(PriceFormation.PERCENT_OVER_COST, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.valueOf(20))));

		BigDecimal resolvedBefore = table.resolvePrice(PRODUCT_REF, new BigDecimal("100.00"), null);
		BigDecimal resolvedAfterCostIncrease = table.resolvePrice(PRODUCT_REF, new BigDecimal("200.00"), null);

		assertThat(resolvedBefore).isEqualByComparingTo("120.00");
		assertThat(resolvedAfterCostIncrease).isEqualByComparingTo("240.00");
	}

	@Test
	void shouldResolvePercentOverBaseFromCurrentProductBasePrice() {
		PriceTable table = table(PriceFormation.PERCENT_OVER_BASE, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.valueOf(-10))));

		BigDecimal resolved = table.resolvePrice(PRODUCT_REF, null, new BigDecimal("100.00"));

		assertThat(resolved).isEqualByComparingTo("90.00");
	}

	@Test
	void shouldRejectFixedEntryWithNonPositiveValue() {
		assertThatThrownBy(() -> table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.ZERO))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("FIXED");
	}

	@Test
	void shouldAllowDiscountBelowMaxWithBlockBehavior() {
		PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, BigDecimal.valueOf(10),
				MaxDiscountBehavior.BLOCK, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThat(table.evaluateDiscount(BigDecimal.valueOf(5))).isEqualTo(DiscountCheckResult.ALLOWED);
	}

	@Test
	void shouldBlockDiscountExceedingMaxWhenBehaviorIsBlock() {
		PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, BigDecimal.valueOf(10),
				MaxDiscountBehavior.BLOCK, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThatThrownBy(() -> table.evaluateDiscount(BigDecimal.valueOf(15)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("exceeds");
	}

	@Test
	void shouldAlertWithoutBlockingWhenBehaviorIsAlert() {
		PriceTable table = table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, BigDecimal.valueOf(10),
				MaxDiscountBehavior.ALERT, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThatCode(() -> assertThat(table.evaluateDiscount(BigDecimal.valueOf(15))).isEqualTo(DiscountCheckResult.ALERT))
				.doesNotThrowAnyException();
	}

	@Test
	void shouldRejectMaxDiscountPercentWithoutBehavior() {
		assertThatThrownBy(() -> table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, BigDecimal.TEN, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("maxDiscountPercent");
	}

	@Test
	void shouldRejectDuplicateEntryReferences() {
		assertThatThrownBy(() -> table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN), new PriceTableEntry(PRODUCT_REF, BigDecimal.ONE))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Duplicate");
	}

	@Test
	void shouldAllowUnlimitedEntriesAndTablesWithNoUpperBoundEnforced() {
		assertThatCode(() -> table(PriceFormation.FIXED, LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN))))
				.doesNotThrowAnyException();
	}

	private PriceTable table(PriceFormation formation, LocalDate validFrom, LocalDate validTo,
			BigDecimal maxDiscountPercent, MaxDiscountBehavior maxDiscountBehavior, List<PriceTableEntry> entries) {
		return PriceTable.of(PriceTableId.of(UUID.randomUUID()), formation, validFrom, validTo, maxDiscountPercent,
				maxDiscountBehavior, entries);
	}
}
