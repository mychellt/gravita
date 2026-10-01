package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.CashFlowBucket;
import br.gravita.core.domain.finance.CashFlowEntry;
import br.gravita.core.domain.finance.CashFlowGranularity;
import br.gravita.core.domain.finance.CashFlowProjection;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CashFlowProjectionTest {

	// 2026-09-28 is a Monday.
	private static final LocalDate MONDAY = LocalDate.of(2026, 9, 28);

	private static BigDecimal money(String value) {
		return new BigDecimal(value);
	}

	@Test
	@DisplayName("Creates daily buckets for every day of the range, including empty ones")
	void dailyBucketsCoverEveryDayOfTheRangeIncludingEmptyOnes() {
		CashFlowProjection projection = CashFlowProjection.of(CashFlowGranularity.DAILY, MONDAY,
				MONDAY.plusDays(2), null, List.of(CashFlowEntry.realizedInflow(MONDAY.plusDays(2), money("10"))));

		assertThat(projection.getBuckets()).extracting(CashFlowBucket::periodStart).containsExactly(MONDAY,
				MONDAY.plusDays(1), MONDAY.plusDays(2));
		assertThat(projection.getBuckets()).extracting(CashFlowBucket::net)
				.usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
				.containsExactly(BigDecimal.ZERO, BigDecimal.ZERO, money("10"));
	}

	@Test
	@DisplayName("Keeps realized and projected movements apart and accumulates the balance from the opening balance")
	void keepsRealizedAndProjectedMovementsApartAndAccumulatesTheBalanceFromTheOpeningBalance() {
		CashFlowProjection projection = CashFlowProjection.of(CashFlowGranularity.DAILY, MONDAY,
				MONDAY.plusDays(1), money("100"),
				List.of(CashFlowEntry.realizedInflow(MONDAY, money("50")),
						CashFlowEntry.realizedOutflow(MONDAY, money("20")),
						CashFlowEntry.projectedInflow(MONDAY.plusDays(1), money("30")),
						CashFlowEntry.projectedOutflow(MONDAY.plusDays(1), money("300"))));

		CashFlowBucket first = projection.getBuckets().get(0);
		assertThat(first.realizedInflow()).isEqualByComparingTo("50");
		assertThat(first.realizedOutflow()).isEqualByComparingTo("20");
		assertThat(first.projectedInflow()).isEqualByComparingTo("0");
		assertThat(first.balance()).isEqualByComparingTo("130");
		CashFlowBucket second = projection.getBuckets().get(1);
		assertThat(second.projectedInflow()).isEqualByComparingTo("30");
		assertThat(second.projectedOutflow()).isEqualByComparingTo("300");
		assertThat(second.balance()).isEqualByComparingTo("-140");
		assertThat(projection.getOpeningBalance()).isEqualByComparingTo("100");
		assertThat(projection.getClosingBalance()).isEqualByComparingTo("-140");
	}

	@Test
	@DisplayName("Runs weekly buckets Monday to Sunday, the first starting on the Monday of the range start")
	void weeklyBucketsRunMondayToSundayAndTheFirstOneStartsOnTheMondayOfTheRangeStart() {
		LocalDate wednesday = MONDAY.plusDays(2);
		CashFlowProjection projection = CashFlowProjection.of(CashFlowGranularity.WEEKLY, wednesday,
				MONDAY.plusDays(8), null, List.of(CashFlowEntry.projectedInflow(MONDAY.plusDays(6), money("5")),
						CashFlowEntry.projectedInflow(MONDAY.plusDays(7), money("7"))));

		assertThat(projection.getBuckets()).hasSize(2);
		assertThat(projection.getBuckets().get(0).periodStart()).isEqualTo(MONDAY);
		assertThat(projection.getBuckets().get(0).periodEnd()).isEqualTo(MONDAY.plusDays(6));
		assertThat(projection.getBuckets().get(0).projectedInflow()).isEqualByComparingTo("5");
		assertThat(projection.getBuckets().get(1).periodStart()).isEqualTo(MONDAY.plusDays(7));
		assertThat(projection.getBuckets().get(1).projectedInflow()).isEqualByComparingTo("7");
	}

	@Test
	@DisplayName("Runs monthly buckets from the first to the last day of the month")
	void monthlyBucketsRunFirstToLastDayOfTheMonth() {
		CashFlowProjection projection = CashFlowProjection.of(CashFlowGranularity.MONTHLY, LocalDate.of(2026, 1, 31),
				LocalDate.of(2026, 3, 1), null,
				List.of(CashFlowEntry.projectedOutflow(LocalDate.of(2026, 2, 28), money("9"))));

		assertThat(projection.getBuckets()).extracting(CashFlowBucket::periodStart).containsExactly(
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));
		assertThat(projection.getBuckets().get(1).periodEnd()).isEqualTo(LocalDate.of(2026, 2, 28));
		assertThat(projection.getBuckets().get(1).projectedOutflow()).isEqualByComparingTo("9");
	}

	@Test
	@DisplayName("Leaves out entries dated outside the range")
	void leavesOutEntriesDatedOutsideTheRange() {
		CashFlowProjection projection = CashFlowProjection.of(CashFlowGranularity.DAILY, MONDAY, MONDAY, null,
				List.of(CashFlowEntry.realizedInflow(MONDAY.minusDays(1), money("10")),
						CashFlowEntry.projectedInflow(MONDAY.plusDays(1), money("10"))));

		assertThat(projection.getClosingBalance()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("Ignores periods that are already over when finding the first negative bucket")
	void theFirstNegativeBucketIgnoresPeriodsThatAreAlreadyOver() {
		CashFlowProjection projection = CashFlowProjection.of(CashFlowGranularity.DAILY, MONDAY,
				MONDAY.plusDays(3), null,
				List.of(CashFlowEntry.realizedOutflow(MONDAY, money("10")),
						CashFlowEntry.projectedInflow(MONDAY.plusDays(1), money("50")),
						CashFlowEntry.projectedOutflow(MONDAY.plusDays(2), money("80"))));

		assertThat(projection.firstNegativeBucket(MONDAY.plusDays(1))).get()
				.extracting(CashFlowBucket::periodStart).isEqualTo(MONDAY.plusDays(2));
		assertThat(projection.firstNegativeBucket(MONDAY)).get().extracting(CashFlowBucket::periodStart)
				.isEqualTo(MONDAY);
		assertThat(projection.firstNegativeBucket(MONDAY.plusDays(4))).isEmpty();
		assertThat(projection.lowestBalance(MONDAY.plusDays(1))).get().isEqualTo(money("-40"));
	}

	@Test
	@DisplayName("Does not treat a balance of exactly zero as negative")
	void aBalanceOfExactlyZeroIsNotNegative() {
		CashFlowProjection projection = CashFlowProjection.of(CashFlowGranularity.DAILY, MONDAY, MONDAY,
				money("10"), List.of(CashFlowEntry.projectedOutflow(MONDAY, money("10"))));

		assertThat(projection.firstNegativeBucket(MONDAY)).isEmpty();
	}

	@Test
	@DisplayName("Rejects a range that ends before it starts")
	void rejectsARangeThatEndsBeforeItStarts() {
		assertThatThrownBy(() -> CashFlowProjection.of(CashFlowGranularity.DAILY, MONDAY, MONDAY.minusDays(1), null,
				List.of())).isInstanceOf(BusinessRuleException.class);
	}
}
