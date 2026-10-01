package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.finance.AgingBucket;
import br.gravita.core.domain.finance.AgingEntry;
import br.gravita.core.domain.finance.AgingRange;
import br.gravita.core.domain.finance.AgingReport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AgingReportTest {

	private static final LocalDate AS_OF = LocalDate.of(2026, 9, 28);

	private static AgingEntry dueDaysAgo(int days, String outstanding) {
		return new AgingEntry(AS_OF.minusDays(days), new BigDecimal(outstanding));
	}

	private static AgingBucket bucket(AgingReport report, AgingRange range) {
		return report.getBuckets().stream().filter(bucket -> bucket.range() == range).findFirst().orElseThrow();
	}

	@ParameterizedTest
	@CsvSource({ "0,UP_TO_30", "1,UP_TO_30", "30,UP_TO_30", "31,FROM_31_TO_60", "60,FROM_31_TO_60",
			"61,FROM_61_TO_90", "90,FROM_61_TO_90", "91,OVER_90", "5000,OVER_90" })
	@DisplayName("Includes both boundaries of each aging range")
	void rangeBoundariesAreInclusive(long days, AgingRange expected) {
		assertThat(AgingRange.of(days)).contains(expected);
	}

	@Test
	@DisplayName("Assigns no aging range to a title that is not due yet")
	void aTitleNotDueYetHasNoRange() {
		assertThat(AgingRange.of(-1)).isEqualTo(Optional.empty());
	}

	@Test
	@DisplayName("Buckets each title by days overdue as of the report date")
	void bucketsEachTitleByDaysOverdueAsOfTheReportDate() {
		AgingReport report = AgingReport.of(AS_OF, List.of(dueDaysAgo(0, "1.00"), dueDaysAgo(30, "2.00"),
				dueDaysAgo(31, "4.00"), dueDaysAgo(60, "8.00"), dueDaysAgo(61, "16.00"), dueDaysAgo(90, "32.00"),
				dueDaysAgo(91, "64.00"), dueDaysAgo(400, "128.00")));

		assertThat(bucket(report, AgingRange.UP_TO_30).total()).isEqualByComparingTo("3.00");
		assertThat(bucket(report, AgingRange.UP_TO_30).titleCount()).isEqualTo(2);
		assertThat(bucket(report, AgingRange.FROM_31_TO_60).total()).isEqualByComparingTo("12.00");
		assertThat(bucket(report, AgingRange.FROM_61_TO_90).total()).isEqualByComparingTo("48.00");
		assertThat(bucket(report, AgingRange.OVER_90).total()).isEqualByComparingTo("192.00");
		assertThat(report.getTotal()).isEqualByComparingTo("255.00");
		assertThat(report.getTitleCount()).isEqualTo(8);
	}

	@Test
	@DisplayName("Lists every range in order, including empty ones")
	void listsEveryRangeInOrderEvenWhenEmpty() {
		AgingReport report = AgingReport.of(AS_OF, List.of());

		assertThat(report.getBuckets()).extracting(AgingBucket::range).containsExactly(AgingRange.UP_TO_30,
				AgingRange.FROM_31_TO_60, AgingRange.FROM_61_TO_90, AgingRange.OVER_90);
		assertThat(report.getTotal()).isEqualByComparingTo("0");
		assertThat(report.getTitleCount()).isZero();
	}

	@Test
	@DisplayName("Leaves out titles not yet due and fully paid ones")
	void leavesOutTitlesNotDueYetAndFullyPaidOnes() {
		AgingReport report = AgingReport.of(AS_OF, List.of(dueDaysAgo(-1, "10.00"), dueDaysAgo(5, "0.00")));

		assertThat(report.getTitleCount()).isZero();
		assertThat(report.getTotal()).isEqualByComparingTo("0");
	}
}
