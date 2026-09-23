package br.gravita.purchasing.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseOrderTest {

	@Test
	void aNewlyCreatedOrderStartsOpen() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false);

		assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.OPEN);
	}

	@Test
	void totalValueSumsEachLinesQuantityTimesUnitPrice() {
		PurchaseOrder order = create(List.of(
				new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN, new BigDecimal("2.00")),
				new PurchaseOrderItem(UUID.randomUUID(), new BigDecimal("3"), new BigDecimal("1.50"))), false);

		assertThat(order.totalValue()).isEqualByComparingTo("24.50");
	}

	@Test
	void carriesTheApprovalRequiredFlagItWasCreatedWith() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE,
				BigDecimal.TEN)), true);

		assertThat(order.isApprovalRequired()).isTrue();
	}

	@Test
	void rejectsAnEmptyItemList() {
		assertThatThrownBy(() -> create(List.of(), false))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one item");
	}

	@Test
	void rejectsANegativeUnitPrice() {
		assertThatThrownBy(() -> new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("-1")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("cannot be negative");
	}

	@Test
	void aFullyReceivedOrderCloses() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false);

		PurchaseOrder updated = order.afterReceiptConfirmed(true);

		assertThat(updated.getStatus()).isEqualTo(PurchaseOrderStatus.CLOSED);
	}

	@Test
	void aPartiallyReceivedOrderStaysPartiallyReceived() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false);

		PurchaseOrder updated = order.afterReceiptConfirmed(false);

		assertThat(updated.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
	}

	@Test
	void confirmingAReceiptAgainstAClosedOrderIsRejected() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false).afterReceiptConfirmed(true);

		assertThatThrownBy(() -> order.afterReceiptConfirmed(true))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CLOSED");
	}

	@Test
	void anOpenOrderNotRequiringApprovalIsReceivable() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false);

		assertThatCode(order::assertReceivable).doesNotThrowAnyException();
	}

	@Test
	void aPartiallyReceivedOrderIsStillReceivable() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false).afterReceiptConfirmed(false);

		assertThatCode(order::assertReceivable).doesNotThrowAnyException();
	}

	@Test
	void aClosedOrderCannotBeReceivedAgainst() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false).afterReceiptConfirmed(true);

		assertThatThrownBy(order::assertReceivable)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CLOSED");
	}

	@Test
	void anOrderRequiringApprovalCannotBeReceivedAgainst() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), true);

		assertThatThrownBy(order::assertReceivable)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("pending approval");
	}

	private PurchaseOrder create(List<PurchaseOrderItem> items, boolean approvalRequired) {
		return PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()), PurchaseRequestId.of(UUID.randomUUID()),
				null, SupplierId.of(UUID.randomUUID()), items, approvalRequired);
	}
}
