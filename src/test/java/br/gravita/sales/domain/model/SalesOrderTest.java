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

	@Test
	void cancellingADraftOrderStoresTheReasonAndDoesNotRequireAnActiveStockReservation() {
		SalesOrder order = draftOrder();

		SalesOrder cancelled = order.cancel("Customer requested cancellation");

		assertThat(cancelled.getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);
		assertThat(cancelled.getCancelReason()).isEqualTo("Customer requested cancellation");
		assertThat(order.hasActiveStockReservation()).isFalse();
	}

	@Test
	void approvedAndInSeparationOrdersHaveAnActiveStockReservation() {
		SalesOrder approved = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.APPROVED, List.of(UUID.randomUUID()), null);
		SalesOrder inSeparation = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.IN_SEPARATION, List.of(UUID.randomUUID()), null);

		assertThat(approved.hasActiveStockReservation()).isTrue();
		assertThat(inSeparation.hasActiveStockReservation()).isTrue();

		SalesOrder cancelled = approved.cancel("Out of stock");
		assertThat(cancelled.getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);
	}

	@Test
	void rejectsCancellingAnInvoicedOrderPointingToTheReturnFlow() {
		SalesOrder invoiced = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.INVOICED, List.of(), null);

		assertThatThrownBy(() -> invoiced.cancel("Changed my mind"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("return flow");
	}

	@Test
	void rejectsCancellingAnAlreadyCancelledOrder() {
		SalesOrder cancelled = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.CANCELLED, List.of(), "Already cancelled");

		assertThatThrownBy(() -> cancelled.cancel("Cancel again")).isInstanceOf(BusinessRuleException.class);
	}

	private static SalesOrder draftOrder() {
		return SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)));
	}

	private static SalesOrderItem item(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount) {
		return new SalesOrderItem(UUID.randomUUID(), quantity, unitPrice, discount);
	}
}
