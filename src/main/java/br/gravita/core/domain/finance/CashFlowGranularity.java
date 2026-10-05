package br.gravita.core.domain.finance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** The width of one bucket of a {@link CashFlowProjection}; weeks run Monday to Sunday. */
public enum CashFlowGranularity {
	DAILY,
	WEEKLY,
	MONTHLY;

	/** The first day of the bucket that contains {@code date}. */
	public LocalDate bucketStart(final LocalDate date) {
		return switch (this) {
			case DAILY -> date;
			case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			case MONTHLY -> date.withDayOfMonth(1);
		};
	}

	/** The last day of the bucket that starts on {@code bucketStart}. */
	public LocalDate bucketEnd(final LocalDate bucketStart) {
		return switch (this) {
			case DAILY -> bucketStart;
			case WEEKLY -> bucketStart.plusDays(6);
			case MONTHLY -> bucketStart.with(TemporalAdjusters.lastDayOfMonth());
		};
	}

	public LocalDate nextBucketStart(final LocalDate bucketStart) {
		return bucketEnd(bucketStart).plusDays(1);
	}
}
