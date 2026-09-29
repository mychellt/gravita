package br.gravita.core.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/**
 * Read model: what customers still owe, bucketed by how many days overdue it
 * is as of {@code asOfDate}. Every {@link AgingRange} is present, empty ones
 * included, in ascending order. Immutable.
 */
@Getter
public final class AgingReport {

	private final LocalDate asOfDate;
	private final List<AgingBucket> buckets;

	private AgingReport(LocalDate asOfDate, List<AgingBucket> buckets) {
		this.asOfDate = asOfDate;
		this.buckets = List.copyOf(buckets);
	}

	/**
	 * Buckets {@code entries} by days overdue as of {@code asOfDate}. An entry
	 * that is not due yet, or has nothing left to pay, is left out.
	 */
	public static AgingReport of(LocalDate asOfDate, List<AgingEntry> entries) {
		Objects.requireNonNull(asOfDate, "asOfDate is required");
		Map<AgingRange, BigDecimal> totals = new EnumMap<>(AgingRange.class);
		Map<AgingRange, Integer> counts = new EnumMap<>(AgingRange.class);
		for (AgingRange range : AgingRange.values()) {
			totals.put(range, BigDecimal.ZERO);
			counts.put(range, 0);
		}
		for (AgingEntry entry : entries) {
			if (entry.outstanding().signum() <= 0) {
				continue;
			}
			AgingRange.of(ChronoUnit.DAYS.between(entry.dueDate(), asOfDate)).ifPresent(range -> {
				totals.merge(range, entry.outstanding(), BigDecimal::add);
				counts.merge(range, 1, Integer::sum);
			});
		}
		List<AgingBucket> buckets = new ArrayList<>();
		for (AgingRange range : AgingRange.values()) {
			buckets.add(new AgingBucket(range, counts.get(range), totals.get(range)));
		}
		return new AgingReport(asOfDate, buckets);
	}

	public BigDecimal getTotal() {
		return buckets.stream().map(AgingBucket::total).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public int getTitleCount() {
		return buckets.stream().mapToInt(AgingBucket::titleCount).sum();
	}
}
