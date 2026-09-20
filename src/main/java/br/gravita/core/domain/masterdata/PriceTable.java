package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;

/**
 * Price table aggregate (UC-M1-13). There is no cap on the number of tables
 * an instance may hold — that constraint simply isn't enforced here.
 * {@code validTo} is not a switch a user has to flip: {@link #isActive} is
 * computed from the current date on every read, so an expired table drops
 * out of price resolution the moment its window closes. Percent-based
 * formations ({@link #resolvePrice}) take the product's current cost/base
 * price as parameters rather than caching a computed value, so later
 * product-price changes propagate automatically at resolution time.
 */
@Getter
public final class PriceTable {

	private final PriceTableId id;
	private final PriceFormation formation;
	private final LocalDate validFrom;
	private final LocalDate validTo;
	private final BigDecimal maxDiscountPercent;
	private final MaxDiscountBehavior maxDiscountBehavior;
	private final List<PriceTableEntry> entries;

	private PriceTable(PriceTableId id, PriceFormation formation, LocalDate validFrom, LocalDate validTo,
			BigDecimal maxDiscountPercent, MaxDiscountBehavior maxDiscountBehavior, List<PriceTableEntry> entries) {
		this.id = Objects.requireNonNull(id, "PriceTableId is required");
		this.formation = Objects.requireNonNull(formation, "Price formation is required");
		this.validFrom = Objects.requireNonNull(validFrom, "validFrom is required");
		this.validTo = requireValidToNotBeforeValidFrom(validFrom, validTo);
		this.maxDiscountPercent = requireValidMaxDiscountPercent(maxDiscountPercent);
		this.maxDiscountBehavior = requireConsistentMaxDiscountBehavior(maxDiscountPercent, maxDiscountBehavior);
		this.entries = requireValidEntries(formation, entries);
	}

	public static PriceTable of(PriceTableId id, PriceFormation formation, LocalDate validFrom, LocalDate validTo,
			BigDecimal maxDiscountPercent, MaxDiscountBehavior maxDiscountBehavior, List<PriceTableEntry> entries) {
		return new PriceTable(id, formation, validFrom, validTo, maxDiscountPercent, maxDiscountBehavior, entries);
	}

	/**
	 * Whether this table should be considered by price resolution on
	 * {@code referenceDate}. A table with a past {@code validTo} answers
	 * {@code false} with no manual deactivation step involved.
	 */
	public boolean isActive(LocalDate referenceDate) {
		Objects.requireNonNull(referenceDate, "referenceDate is required");
		if (referenceDate.isBefore(validFrom)) {
			return false;
		}
		return validTo == null || !referenceDate.isAfter(validTo);
	}

	/**
	 * Resolves the sale price for {@code ref} using the product's *current*
	 * cost/base price, passed in by the caller at resolution time rather than
	 * read from this table — see the class javadoc.
	 */
	public BigDecimal resolvePrice(ProductOrClassRef ref, BigDecimal productAverageCost, BigDecimal productBasePrice) {
		PriceTableEntry entry = findEntry(ref);
		return switch (formation) {
			case FIXED -> entry.value();
			case PERCENT_OVER_COST -> applyPercent(requireProductValue(productAverageCost, "average cost"), entry.value());
			case PERCENT_OVER_BASE -> applyPercent(requireProductValue(productBasePrice, "base price"), entry.value());
		};
	}

	/**
	 * Checks a requested sales discount against {@link #maxDiscountPercent}.
	 * A {@code BLOCK} table throws once the limit is exceeded; an
	 * {@code ALERT} table returns {@link DiscountCheckResult#ALERT} so the
	 * caller (sales) can surface a warning to the salesperson while still
	 * allowing the line.
	 */
	public DiscountCheckResult evaluateDiscount(BigDecimal requestedDiscountPercent) {
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

	private PriceTableEntry findEntry(ProductOrClassRef ref) {
		return entries.stream()
				.filter(entry -> entry.ref().equals(ref))
				.findFirst()
				.orElseThrow(() -> new BusinessRuleException("No price table entry for reference: " + ref));
	}

	private static BigDecimal applyPercent(BigDecimal base, BigDecimal percent) {
		BigDecimal factor = BigDecimal.ONE.add(percent.divide(BigDecimal.valueOf(100)));
		return base.multiply(factor);
	}

	private static BigDecimal requireProductValue(BigDecimal value, String fieldName) {
		if (value == null) {
			throw new BusinessRuleException("Product " + fieldName + " is required to resolve this price table's entries");
		}
		return value;
	}

	private static LocalDate requireValidToNotBeforeValidFrom(LocalDate validFrom, LocalDate validTo) {
		if (validTo != null && validTo.isBefore(validFrom)) {
			throw new BusinessRuleException("validTo cannot be before validFrom");
		}
		return validTo;
	}

	private static BigDecimal requireValidMaxDiscountPercent(BigDecimal maxDiscountPercent) {
		if (maxDiscountPercent == null) {
			return null;
		}
		if (maxDiscountPercent.compareTo(BigDecimal.ZERO) < 0 || maxDiscountPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
			throw new BusinessRuleException("maxDiscountPercent must be between 0 and 100: " + maxDiscountPercent);
		}
		return maxDiscountPercent;
	}

	private static MaxDiscountBehavior requireConsistentMaxDiscountBehavior(BigDecimal maxDiscountPercent,
			MaxDiscountBehavior maxDiscountBehavior) {
		if ((maxDiscountPercent == null) != (maxDiscountBehavior == null)) {
			throw new BusinessRuleException("maxDiscountPercent and maxDiscountBehavior must be set together");
		}
		return maxDiscountBehavior;
	}

	private static List<PriceTableEntry> requireValidEntries(PriceFormation formation, List<PriceTableEntry> entries) {
		List<PriceTableEntry> copy = entries == null ? List.of() : List.copyOf(entries);
		Set<ProductOrClassRef> seenRefs = new HashSet<>();
		for (PriceTableEntry entry : copy) {
			if (!seenRefs.add(entry.ref())) {
				throw new BusinessRuleException("Duplicate price table entry for reference: " + entry.ref());
			}
			validateEntryValue(formation, entry);
		}
		return copy;
	}

	private static void validateEntryValue(PriceFormation formation, PriceTableEntry entry) {
		BigDecimal value = entry.value();
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
