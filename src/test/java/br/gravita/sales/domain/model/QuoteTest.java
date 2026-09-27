package br.gravita.sales.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.QuoteItem;
import br.gravita.core.domain.sales.QuoteStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 9, 27);

	@Test
	void aNewlyCreatedQuoteStartsAsDraftWithTheGivenValidity() {
		LocalDate validUntil = TODAY.plusDays(15);

		Quote quote = create(List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)), validUntil);

		assertThat(quote.getStatus()).isEqualTo(QuoteStatus.DRAFT);
		assertThat(quote.getValidUntil()).isEqualTo(validUntil);
	}

	@Test
	void keepsItemsPricesAndDiscountsExactlyAsSubmitted() {
		QuoteItem first = item(new BigDecimal("3"), new BigDecimal("12.34"), new BigDecimal("1.50"));
		QuoteItem second = item(new BigDecimal("0.5"), new BigDecimal("99.90"), BigDecimal.ZERO);

		Quote quote = create(List.of(first, second), TODAY.plusDays(1));

		assertThat(quote.getItems()).containsExactly(first, second);
	}

	@Test
	void doesNotShareTheCallersItemList() {
		List<QuoteItem> items = new ArrayList<>(List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)));

		Quote quote = create(items, TODAY.plusDays(1));
		items.clear();

		assertThat(quote.getItems()).hasSize(1);
	}

	@Test
	void totalValueSumsEachLinesSubtotalMinusItsDiscount() {
		Quote quote = create(List.of(
				item(BigDecimal.TEN, new BigDecimal("2.00"), new BigDecimal("5.00")),
				item(new BigDecimal("3"), new BigDecimal("1.50"), BigDecimal.ZERO)), TODAY.plusDays(1));

		assertThat(quote.totalValue()).isEqualByComparingTo("19.50");
	}

	@Test
	void rejectsAnEmptyItemList() {
		assertThatThrownBy(() -> create(List.of(), TODAY.plusDays(1)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one item");
	}

	@Test
	void rejectsANullItemList() {
		assertThatThrownBy(() -> create(null, TODAY.plusDays(1)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one item");
	}

	@Test
	void rejectsAValidityDateOfToday() {
		assertThatThrownBy(() -> create(List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)), TODAY))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("validUntil must be in the future");
	}

	@Test
	void rejectsAValidityDateInThePast() {
		assertThatThrownBy(() -> create(List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				TODAY.minusDays(1)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("validUntil must be in the future");
	}

	@Test
	void anItemWithoutADiscountDefaultsToZero() {
		QuoteItem item = new QuoteItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN, null);

		assertThat(item.discount()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void rejectsANonPositiveQuantity() {
		assertThatThrownBy(() -> item(BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("quantity must be positive");
	}

	@Test
	void rejectsANegativeUnitPrice() {
		assertThatThrownBy(() -> item(BigDecimal.ONE, new BigDecimal("-1"), BigDecimal.ZERO))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("unitPrice cannot be negative");
	}

	@Test
	void rejectsANegativeDiscount() {
		assertThatThrownBy(() -> item(BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("-0.01")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("discount cannot be negative");
	}

	@Test
	void rejectsADiscountGreaterThanTheLineSubtotal() {
		assertThatThrownBy(() -> item(new BigDecimal("2"), BigDecimal.TEN, new BigDecimal("20.01")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("cannot exceed the item subtotal");
	}

	@Test
	void convertingADraftQuoteTransitionsItToConvertedAndKeepsItsData() {
		QuoteItem item = item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO);
		Quote quote = create(List.of(item), TODAY.plusDays(1));

		Quote converted = quote.convert(TODAY);

		assertThat(converted.getStatus()).isEqualTo(QuoteStatus.CONVERTED);
		assertThat(converted.getId()).isEqualTo(quote.getId());
		assertThat(converted.getCustomerId()).isEqualTo(quote.getCustomerId());
		assertThat(converted.getItems()).containsExactly(item);
		assertThat(converted.getValidUntil()).isEqualTo(quote.getValidUntil());
	}

	@Test
	void convertingOnTheLastValidDayIsAllowed() {
		Quote quote = Quote.of(QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
				List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)), TODAY, QuoteStatus.DRAFT);

		Quote converted = quote.convert(TODAY);

		assertThat(converted.getStatus()).isEqualTo(QuoteStatus.CONVERTED);
	}

	@Test
	void rejectsConvertingAnAlreadyConvertedQuote() {
		Quote quote = create(List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)), TODAY.plusDays(1))
				.convert(TODAY);

		assertThatThrownBy(() -> quote.convert(TODAY))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already converted");
	}

	@Test
	void rejectsConvertingAnExpiredQuote() {
		Quote quote = Quote.of(QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
				List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)), TODAY.minusDays(1),
				QuoteStatus.DRAFT);

		assertThatThrownBy(() -> quote.convert(TODAY))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("expired");
	}

	private static QuoteItem item(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount) {
		return new QuoteItem(UUID.randomUUID(), quantity, unitPrice, discount);
	}

	private static Quote create(List<QuoteItem> items, LocalDate validUntil) {
		return Quote.create(QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(), items, validUntil,
				TODAY);
	}
}
