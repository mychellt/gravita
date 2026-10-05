package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Read model: the realized entries and exits of the settlements plus the
 * projection of the open titles, bucketed by date. Every bucket between
 * {@code from} and {@code to} is present, empty ones included. Immutable.
 */
public final class CashFlowProjection {

	private final CashFlowGranularity granularity;
	private final LocalDate from;
	private final LocalDate to;
	private final BigDecimal openingBalance;
	private final List<CashFlowBucket> buckets;

	private CashFlowProjection(final CashFlowGranularity granularity, final LocalDate from, final LocalDate to,
			final BigDecimal openingBalance, final List<CashFlowBucket> buckets) {
		this.granularity = granularity;
		this.from = from;
		this.to = to;
		this.openingBalance = openingBalance;
		this.buckets = List.copyOf(buckets);
	}

	/**
	 * Buckets {@code entries} over {@code from}..{@code to} (both inclusive);
	 * an entry dated outside that range is left out.
	 */
	public static CashFlowProjection of(final CashFlowGranularity granularity, final LocalDate from, final LocalDate to,
			final BigDecimal openingBalance, final List<CashFlowEntry> entries) {
		Objects.requireNonNull(granularity, "granularity is required");
		Objects.requireNonNull(from, "from is required");
		Objects.requireNonNull(to, "to is required");
		if (to.isBefore(from)) {
			throw new BusinessRuleException("to must not be before from: " + from + " > " + to);
		}
		final BigDecimal opening = openingBalance == null ? BigDecimal.ZERO : openingBalance;

		final TreeMap<LocalDate, BigDecimal[]> totals = new TreeMap<>();
		for (LocalDate start = granularity.bucketStart(from); !start.isAfter(to); start = granularity
				.nextBucketStart(start)) {
			totals.put(start, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
		}
		for (final CashFlowEntry entry : entries) {
			if (entry.date().isBefore(from) || entry.date().isAfter(to)) {
				continue;
			}
			final BigDecimal[] bucket = totals.get(granularity.bucketStart(entry.date()));
			final boolean inflow = entry.direction() == CashFlowEntry.Direction.INFLOW;
			final int slot = (entry.realized() ? 0 : 2) + (inflow ? 0 : 1);
			bucket[slot] = bucket[slot].add(entry.amount());
		}

		final List<CashFlowBucket> buckets = new ArrayList<>(totals.size());
		BigDecimal balance = opening;
		for (final var bucket : totals.entrySet()) {
			final BigDecimal[] t = bucket.getValue();
			balance = balance.add(t[0]).add(t[2]).subtract(t[1]).subtract(t[3]);
			buckets.add(new CashFlowBucket(bucket.getKey(), granularity.bucketEnd(bucket.getKey()), t[0], t[1], t[2],
					t[3], balance));
		}
		return new CashFlowProjection(granularity, from, to, opening, buckets);
	}

	/**
	 * The first bucket that is not over yet as of {@code today} and closes with
	 * a negative balance, if any: the point from which the projection says the
	 * cash runs out.
	 */
	public Optional<CashFlowBucket> firstNegativeBucket(final LocalDate today) {
		return buckets.stream().filter(bucket -> !bucket.periodEnd().isBefore(today))
				.filter(bucket -> bucket.balance().signum() < 0).findFirst();
	}

	/** The lowest closing balance among the buckets that are not over yet as of {@code today}. */
	public Optional<BigDecimal> lowestBalance(final LocalDate today) {
		return buckets.stream().filter(bucket -> !bucket.periodEnd().isBefore(today)).map(CashFlowBucket::balance)
				.min(BigDecimal::compareTo);
	}

	public CashFlowGranularity getGranularity() {
		return granularity;
	}

	public LocalDate getFrom() {
		return from;
	}

	public LocalDate getTo() {
		return to;
	}

	public BigDecimal getOpeningBalance() {
		return openingBalance;
	}

	public List<CashFlowBucket> getBuckets() {
		return buckets;
	}

	/** The balance at the end of the last bucket. */
	public BigDecimal getClosingBalance() {
		return buckets.isEmpty() ? openingBalance : buckets.get(buckets.size() - 1).balance();
	}
}
