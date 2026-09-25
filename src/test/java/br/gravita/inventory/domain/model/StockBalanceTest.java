package br.gravita.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockBalanceTest {

	@Test
	void availableIsOnHandMinusReserved() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("100"), new BigDecimal("30"), BigDecimal.ZERO,
				new BigDecimal("12.50"));

		assertThat(balance.available()).isEqualByComparingTo("70");
	}

	@Test
	void reserveIncreasesReservedAndLeavesOnHandUntouched() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("100"), new BigDecimal("30"), BigDecimal.ZERO,
				new BigDecimal("12.50"));

		StockBalance reserved = balance.reserve(new BigDecimal("20"));

		assertThat(reserved.getOnHand()).isEqualByComparingTo("100");
		assertThat(reserved.getReserved()).isEqualByComparingTo("50");
		assertThat(reserved.available()).isEqualByComparingTo("50");
	}

	@Test
	void reserveRejectsAQuantityGreaterThanAvailable() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("100"), new BigDecimal("90"), BigDecimal.ZERO,
				new BigDecimal("12.50"));

		assertThatThrownBy(() -> balance.reserve(new BigDecimal("20")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Insufficient available stock");
	}

	@Test
	void reserveRejectsAZeroOrNegativeQuantity() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO,
				new BigDecimal("12.50"));

		assertThatThrownBy(() -> balance.reserve(BigDecimal.ZERO)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> balance.reserve(new BigDecimal("-5"))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void receiveEntryRecalculatesAverageCostAsAWeightedAverage() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("12.50"));

		StockBalance afterEntry = balance.receiveEntry(new BigDecimal("50"), new BigDecimal("14.00"));

		assertThat(afterEntry.getOnHand()).isEqualByComparingTo("150");
		// (100*12.50 + 50*14.00) / 150 = 13.00
		assertThat(afterEntry.getAverageCost()).isEqualByComparingTo("13.00");
	}

	@Test
	void receiveEntryLeavesReservedAndInTransitUntouched() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("5"),
				new BigDecimal("12.50"));

		StockBalance afterEntry = balance.receiveEntry(new BigDecimal("10"), new BigDecimal("12.50"));

		assertThat(afterEntry.getReserved()).isEqualByComparingTo("30");
		assertThat(afterEntry.getInTransit()).isEqualByComparingTo("5");
	}

	@Test
	void exitDecreasesOnHandOnlyAndRejectsAZeroOrNegativeQuantity() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO,
				new BigDecimal("12.50"));

		StockBalance afterExit = balance.exit(new BigDecimal("40"));

		assertThat(afterExit.getOnHand()).isEqualByComparingTo("60");
		assertThat(afterExit.getReserved()).isEqualByComparingTo("10");
		assertThatThrownBy(() -> balance.exit(BigDecimal.ZERO)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> balance.exit(new BigDecimal("-1"))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void consumeReservedDecreasesOnHandAndReservedTogetherLeavingAvailableUnchanged() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("100"), new BigDecimal("30"), BigDecimal.ZERO,
				new BigDecimal("12.50"));

		StockBalance afterExit = balance.consumeReserved(new BigDecimal("20"));

		assertThat(afterExit.getOnHand()).isEqualByComparingTo("80");
		assertThat(afterExit.getReserved()).isEqualByComparingTo("10");
		assertThat(afterExit.available()).isEqualByComparingTo(balance.available());
	}

	@Test
	void applyAdjustmentReflectsTheDeltaExactlyWhetherPositiveOrNegative() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("12.50"));

		assertThat(balance.applyAdjustment(new BigDecimal("5")).getOnHand()).isEqualByComparingTo("105");
		assertThat(balance.applyAdjustment(new BigDecimal("-5")).getOnHand()).isEqualByComparingTo("95");
		assertThatThrownBy(() -> balance.applyAdjustment(BigDecimal.ZERO)).isInstanceOf(BusinessRuleException.class);
	}
}
