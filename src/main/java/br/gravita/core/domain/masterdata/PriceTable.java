package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;

@Getter
public final class PriceTable {

	private final PriceTableId id;
	private final PriceFormation formation;
	private final LocalDate validFrom;
	private final LocalDate validTo;
	private final BigDecimal maxDiscountPercent;
	private final MaxDiscountBehavior maxDiscountBehavior;
	private final List<PriceTableEntry> entries;

	public PriceTable(final PriceTableId id, final PriceFormation formation, final LocalDate validFrom, final LocalDate validTo,
			final BigDecimal maxDiscountPercent, final MaxDiscountBehavior maxDiscountBehavior, final List<PriceTableEntry> entries) {
		this.id = Objects.requireNonNull(id, "PriceTableId is required");
		this.formation = Objects.requireNonNull(formation, "Price formation is required");
		this.validFrom = Objects.requireNonNull(validFrom, "validFrom is required");
		this.validTo = requireValidToNotBeforeValidFrom(validFrom, validTo);
		this.maxDiscountPercent = requireValidMaxDiscountPercent(maxDiscountPercent);
		this.maxDiscountBehavior = requireConsistentMaxDiscountBehavior(maxDiscountPercent, maxDiscountBehavior);
		this.entries = requireValidEntries(formation, entries);
	}

	public static PriceTable of(final PriceTableId id, final PriceFormation formation, final LocalDate validFrom, final LocalDate validTo,
			final BigDecimal maxDiscountPercent, final MaxDiscountBehavior maxDiscountBehavior, final List<PriceTableEntry> entries) {
		return new PriceTable(id, formation, validFrom, validTo, maxDiscountPercent, maxDiscountBehavior, entries);
	}

	public boolean isActive(final LocalDate referenceDate) {
		Objects.requireNonNull(referenceDate, "referenceDate is required");
		if (referenceDate.isBefore(validFrom)) {
			return false;
		}
		return validTo == null || !referenceDate.isAfter(validTo);
	}

	public BigDecimal resolvePrice(final ProductOrClassRef ref, final BigDecimal productAverageCost, final BigDecimal productBasePrice) {
		final PriceTableEntry entry = findEntry(ref);
		return switch (formation) {
			case FIXED -> entry.value();
			case PERCENT_OVER_COST -> applyPercent(requireProductValue(productAverageCost, "average cost"), entry.value());
			case PERCENT_OVER_BASE -> applyPercent(requireProductValue(productBasePrice, "base price"), entry.value());
		};
	}

	public DiscountCheckResult evaluateDiscount(final BigDecimal requestedDiscountPercent) {
		Objects.requireNonNull(requestedDiscountPercent, "requestedDiscountPercent is required");
		if (maxDiscountPercent == null || requestedDiscountPercent.compareTo(maxDiscountPercent) <= 0) {
			return DiscountCheckResult.ALLOWED;
		}
		if (maxDiscountBehavior == MaxDiscountBehavior.BLOCK) {
			throw new BusinessRuleException("Discount of " + requestedDiscountPercent
					+ "% exceeds the maximum of " + maxDiscountPercent + "% allowed by this price table");
		}
		return DiscountCheckResult.ALERT;
	}

	private PriceTableEntry findEntry(final ProductOrClassRef ref) {
		return entries.stream()
				.filter(entry -> entry.ref().equals(ref))
				.findFirst()
				.orElseThrow(() -> new BusinessRuleException("No price table entry for reference: " + ref));
	}

	private static BigDecimal applyPercent(final BigDecimal base, final BigDecimal percent) {
		final BigDecimal factor = BigDecimal.ONE.add(percent.divide(BigDecimal.valueOf(100)));
		return base.multiply(factor);
	}

	private static BigDecimal requireProductValue(final BigDecimal value, final String fieldName) {
		if (value == null) {
			throw new BusinessRuleException("Product " + fieldName + " is required to resolve this price table's entries");
		}
		return value;
	}

	private static LocalDate requireValidToNotBeforeValidFrom(final LocalDate validFrom, final LocalDate validTo) {
		if (validTo != null && validTo.isBefore(validFrom)) {
			throw new BusinessRuleException("validTo cannot be before validFrom");
		}
		return validTo;
	}

	private static BigDecimal requireValidMaxDiscountPercent(final BigDecimal maxDiscountPercent) {
		if (maxDiscountPercent == null) {
			return null;
		}
		if (maxDiscountPercent.compareTo(BigDecimal.ZERO) < 0 || maxDiscountPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
			throw new BusinessRuleException("maxDiscountPercent must be between 0 and 100: " + maxDiscountPercent);
		}
		return maxDiscountPercent;
	}

	private static MaxDiscountBehavior requireConsistentMaxDiscountBehavior(final BigDecimal maxDiscountPercent,
			final MaxDiscountBehavior maxDiscountBehavior) {
		if ((maxDiscountPercent == null) != (maxDiscountBehavior == null)) {
			throw new BusinessRuleException("maxDiscountPercent and maxDiscountBehavior must be set together");
		}
		return maxDiscountBehavior;
	}

	private static List<PriceTableEntry> requireValidEntries(final PriceFormation formation, final List<PriceTableEntry> entries) {
		final List<PriceTableEntry> copy = entries == null ? List.of() : List.copyOf(entries);
		final Set<ProductOrClassRef> seenRefs = new HashSet<>();
		for (final PriceTableEntry entry : copy) {
			if (!seenRefs.add(entry.ref())) {
				throw new BusinessRuleException("Duplicate price table entry for reference: " + entry.ref());
			}
			validateEntryValue(formation, entry);
		}
		return copy;
	}

	private static void validateEntryValue(final PriceFormation formation, final PriceTableEntry entry) {
		final BigDecimal value = entry.value();
		if (formation == PriceFormation.FIXED) {
			if (value.compareTo(BigDecimal.ZERO) <= 0) {
				throw new BusinessRuleException("A FIXED price table entry value must be positive: " + value);
			}
			return;
		}
		if (value.compareTo(BigDecimal.valueOf(-100)) <= 0) {
			throw new BusinessRuleException("A percent-based price table entry cannot discount 100% or more: " + value);
		}
	}
}
