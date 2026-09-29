package br.gravita.core.domain.finance;

import java.util.Optional;

/**
 * The days-overdue ranges of the aging report, both ends inclusive: 0-30,
 * 31-60, 61-90 and more than 90 days. A title due on the report date is 0 days
 * overdue.
 */
public enum AgingRange {

	UP_TO_30(0, 30),
	FROM_31_TO_60(31, 60),
	FROM_61_TO_90(61, 90),
	OVER_90(91, Long.MAX_VALUE);

	private final long fromDays;
	private final long toDays;

	AgingRange(long fromDays, long toDays) {
		this.fromDays = fromDays;
		this.toDays = toDays;
	}

	public long fromDays() {
		return fromDays;
	}

	/** The last day of the range; {@code empty} for the open-ended {@link #OVER_90}. */
	public Optional<Long> toDays() {
		return toDays == Long.MAX_VALUE ? Optional.empty() : Optional.of(toDays);
	}

	/** The range {@code daysOverdue} falls in; {@code empty} for a title that is not due yet (negative days). */
	public static Optional<AgingRange> of(long daysOverdue) {
		for (AgingRange range : values()) {
			if (daysOverdue >= range.fromDays && daysOverdue <= range.toDays) {
				return Optional.of(range);
			}
		}
		return Optional.empty();
	}
}
