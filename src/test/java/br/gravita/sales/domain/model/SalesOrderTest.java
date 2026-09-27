package br.gravita.sales.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesOrderTest {

	@Test
	void aNewlyCreatedOrderStartsAsDraftAndReferencesItsOriginQuote() {
		QuoteId originQuoteId = QuoteId.of(UUID.randomUUID());
		UUID customerId = UUID.randomUUID();
		List<SalesOrderItem> items = List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO));

		SalesOrder order = SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()), originQuoteId, customerId,
				items);

		assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.DRAFT);
		assertThat(order.getOriginQuoteId()).isEqualTo(originQuoteId);
		assertThat(order.getCustomerId()).isEqualTo(customerId);
		assertThat(order.getItems()).containsExactlyElementsOf(items);
	}

	@Test
	void rejectsAnEmptyItemList() {
		assertThatThrownBy(() -> SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()),
				QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), List.of()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one item");
	}

	@Test
	void totalValueSumsEachLinesSubtotalMinusItsDiscount() {
		SalesOrder order = SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()),
				QuoteId.of(UUID.randomUUID()), UUID.randomUUID(),
				List.of(
						item(BigDecimal.TEN, new BigDecimal("2.00"), new BigDecimal("5.00")),
						item(new BigDecimal("3"), new BigDecimal("1.50"), BigDecimal.ZERO)));

		assertThat(order.totalValue()).isEqualByComparingTo("19.50");
	}

	private static SalesOrderItem item(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount) {
		return new SalesOrderItem(UUID.randomUUID(), quantity, unitPrice, discount);
	}
}
