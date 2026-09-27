package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
@Getter
public final class Quote {

	private final QuoteId id;
	private final UUID customerId;
	private final UUID salespersonId;
	private final List<QuoteItem> items;
	private final LocalDate validUntil;
	private final QuoteStatus status;

	public static Quote create(QuoteId id, UUID customerId, UUID salespersonId, List<QuoteItem> items,
			LocalDate validUntil, LocalDate today) {
		Objects.requireNonNull(customerId, "customerId is required");
		Objects.requireNonNull(salespersonId, "salespersonId is required");
		Objects.requireNonNull(validUntil, "validUntil is required");
		if (!validUntil.isAfter(today)) {
			throw new BusinessRuleException("Quote validUntil must be in the future, was " + validUntil);
		}
		return new Quote(id, customerId, salespersonId, requireNonEmptyItems(items), validUntil, QuoteStatus.DRAFT);
	}

	public static Quote of(QuoteId id, UUID customerId, UUID salespersonId, List<QuoteItem> items,
			LocalDate validUntil, QuoteStatus status) {
		return new Quote(id, customerId, salespersonId, items, validUntil, status);
	}

	/**
	 * UC-M7-02: delivers the quote, moving it to {@code SENT}. Rejected up
	 * front - before the caller does any channel-specific delivery work - once
	 * {@code validUntil} has passed; the salesperson must create a new quote
	 * instead.
	 */
	public Quote send(LocalDate today) {
		if (today.isAfter(validUntil)) {
			throw new BusinessRuleException(
					"Quote " + id.value() + " has expired (validUntil: " + validUntil + ")");
		}
		return new Quote(id, customerId, salespersonId, items, validUntil, QuoteStatus.SENT);
	}

	public BigDecimal totalValue() {
		return items.stream().map(QuoteItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Quote convert(LocalDate today) {
		if (status == QuoteStatus.CONVERTED) {
			throw new BusinessRuleException("Quote is already converted: " + id.value());
		}
		if (today.isAfter(validUntil)) {
			throw new BusinessRuleException("Cannot convert an expired quote, validUntil was " + validUntil);
		}
		return new Quote(id, customerId, salespersonId, items, validUntil, QuoteStatus.CONVERTED);
	}

	private static List<QuoteItem> requireNonEmptyItems(List<QuoteItem> items) {
		List<QuoteItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A quote must have at least one item");
		}
		return copy;
	}
}
