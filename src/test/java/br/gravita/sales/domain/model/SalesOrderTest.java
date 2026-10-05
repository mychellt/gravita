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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SalesOrderTest {

	@Test
	@DisplayName("A new order starts as DRAFT and references its originating quote")
	void newlyCreatedOrderStartsAsDraftAndReferencesItsOriginQuote() {
		final QuoteId originQuoteId = QuoteId.of(UUID.randomUUID());
		final UUID customerId = UUID.randomUUID();
		final List<SalesOrderItem> items = List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO));

		final SalesOrder order = SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()), originQuoteId, customerId,
				UUID.randomUUID(), items);

		assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.DRAFT);
		assertThat(order.getOriginQuoteId()).isEqualTo(originQuoteId);
		assertThat(order.getCustomerId()).isEqualTo(customerId);
		assertThat(order.getItems()).containsExactlyElementsOf(items);
	}

	@Test
	@DisplayName("Rejects an order with an empty item list")
	void rejectsAnEmptyItemList() {
		assertThatThrownBy(() -> SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()),
				QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(), List.of()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one item");
	}

	@Test
	@DisplayName("Total value sums each line's subtotal minus its discount")
	void totalValueSumsEachLinesSubtotalMinusItsDiscount() {
		final SalesOrder order = SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()),
				QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
				List.of(
						item(BigDecimal.TEN, new BigDecimal("2.00"), new BigDecimal("5.00")),
						item(new BigDecimal("3"), new BigDecimal("1.50"), BigDecimal.ZERO)));

		assertThat(order.totalValue()).isEqualByComparingTo("19.50");
	}

	@Test
	@DisplayName("Discount percent is the total discount divided by the total subtotal")
	void discountPercentIsTheTotalDiscountOverTheTotalSubtotal() {
		final SalesOrder order = SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()),
				QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
				List.of(item(BigDecimal.ONE, new BigDecimal("100.00"), new BigDecimal("25.00"))));

		assertThat(order.discountPercent()).isEqualByComparingTo("25.0000");
	}

	@Test
	@DisplayName("Approving a draft order moves it to APPROVED and records the approval details")
	void approvingADraftOrderTransitionsItToApprovedAndRecordsTheApprovalDetails() {
		final SalesOrder order = SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()),
				QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
				List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)));
		final UUID approvedBy = UUID.randomUUID();
		final UUID alcadaId = UUID.randomUUID();

		final SalesOrder approved = order.approve(approvedBy, alcadaId);

		assertThat(approved.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
		assertThat(approved.getApprovedBy()).isEqualTo(approvedBy);
		assertThat(approved.getAlcadaId()).isEqualTo(alcadaId);
	}

	@Test
	@DisplayName("Rejects approving an order that is not in DRAFT")
	void rejectsApprovingAnOrderThatIsNotDraft() {
		final SalesOrder approvedOrder = SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()),
				QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
				List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO))).approve(UUID.randomUUID(), null);

		assertThatThrownBy(() -> approvedOrder.approve(UUID.randomUUID(), null))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Only DRAFT orders can be approved");
	}

	@Test
	@DisplayName("Cancelling a draft order stores the reason and does not require an active stock reservation")
	void cancellingADraftOrderStoresTheReasonAndDoesNotRequireAnActiveStockReservation() {
		final SalesOrder order = draftOrder();

		final SalesOrder cancelled = order.cancel("Customer requested cancellation");

		assertThat(cancelled.getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);
		assertThat(cancelled.getCancelReason()).isEqualTo("Customer requested cancellation");
		assertThat(order.hasActiveStockReservation()).isFalse();
	}

	@Test
	@DisplayName("Approved and in-separation orders hold an active stock reservation")
	void approvedAndInSeparationOrdersHaveAnActiveStockReservation() {
		final SalesOrder approved = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.APPROVED, UUID.randomUUID(), null);
		final SalesOrder inSeparation = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.IN_SEPARATION, UUID.randomUUID(), null);

		assertThat(approved.hasActiveStockReservation()).isTrue();
		assertThat(inSeparation.hasActiveStockReservation()).isTrue();

		final SalesOrder cancelled = approved.cancel("Out of stock");
		assertThat(cancelled.getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);
	}

	@Test
	@DisplayName("Rejects cancelling an invoiced order and points to the return flow")
	void rejectsCancellingAnInvoicedOrderPointingToTheReturnFlow() {
		final SalesOrder invoiced = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.INVOICED, null, null);

		assertThatThrownBy(() -> invoiced.cancel("Changed my mind"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("return flow");
	}

	@Test
	@DisplayName("Rejects cancelling an order that is already cancelled")
	void rejectsCancellingAnAlreadyCancelledOrder() {
		final SalesOrder cancelled = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.CANCELLED, null, null, "Already cancelled");

		assertThatThrownBy(() -> cancelled.cancel("Cancel again")).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Invoicing an approved or in-separation order moves it to INVOICED")
	void invoicingAnApprovedOrTheInSeparationOrderTransitionsItToInvoiced() {
		final SalesOrder approved = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.APPROVED, UUID.randomUUID(), null);
		final SalesOrder inSeparation = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.IN_SEPARATION, UUID.randomUUID(), null);

		assertThat(approved.invoice().getStatus()).isEqualTo(SalesOrderStatus.INVOICED);
		assertThat(inSeparation.invoice().getStatus()).isEqualTo(SalesOrderStatus.INVOICED);
	}

	@Test
	@DisplayName("Rejects invoicing a draft order")
	void rejectsInvoicingADraftOrder() {
		final SalesOrder draft = draftOrder();

		assertThatThrownBy(draft::invoice).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Only APPROVED or IN_SEPARATION orders can be invoiced");
	}

	@Test
	@DisplayName("Rejects invoicing an order that is already invoiced")
	void rejectsInvoicingAnAlreadyInvoicedOrder() {
		final SalesOrder invoiced = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.INVOICED, null, null);

		assertThatThrownBy(invoiced::invoice).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Invoicing an order records the invoice date")
	void invoicingAnOrderRecordsTheInvoiceDate() {
		final SalesOrder approved = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.APPROVED, UUID.randomUUID(), null);

		final SalesOrder invoiced = approved.invoice();

		assertThat(invoiced.getInvoicedAt()).isEqualTo(java.time.LocalDate.now());
	}

	private static SalesOrder draftOrder() {
		return SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), List.of(item(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)));
	}

	private static SalesOrderItem item(final BigDecimal quantity, final BigDecimal unitPrice, final BigDecimal discount) {
		return new SalesOrderItem(UUID.randomUUID(), quantity, unitPrice, discount);
	}
}
