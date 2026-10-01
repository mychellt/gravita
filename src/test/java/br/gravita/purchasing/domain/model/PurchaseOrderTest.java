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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PurchaseOrderTest {

	@Test
	@DisplayName("A newly created order starts OPEN")
	void aNewlyCreatedOrderStartsOpen() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false);

		assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.OPEN);
	}

	@Test
	@DisplayName("Total value sums each line's quantity times unit price")
	void totalValueSumsEachLinesQuantityTimesUnitPrice() {
		PurchaseOrder order = create(List.of(
				new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN, new BigDecimal("2.00")),
				new PurchaseOrderItem(UUID.randomUUID(), new BigDecimal("3"), new BigDecimal("1.50"))), false);

		assertThat(order.totalValue()).isEqualByComparingTo("24.50");
	}

	@Test
	@DisplayName("Carries the approval-required flag it was created with")
	void carriesTheApprovalRequiredFlagItWasCreatedWith() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE,
				BigDecimal.TEN)), true);

		assertThat(order.isApprovalRequired()).isTrue();
	}

	@Test
	@DisplayName("Rejects an order with an empty item list")
	void rejectsAnEmptyItemList() {
		assertThatThrownBy(() -> create(List.of(), false))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one item");
	}

	@Test
	@DisplayName("Rejects an order line with a negative unit price")
	void rejectsANegativeUnitPrice() {
		assertThatThrownBy(() -> new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("-1")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("cannot be negative");
	}

	@Test
	@DisplayName("An order whose quantities are fully received is closed")
	void aFullyReceivedOrderCloses() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false);

		PurchaseOrder updated = order.afterReceiptConfirmed(true);

		assertThat(updated.getStatus()).isEqualTo(PurchaseOrderStatus.CLOSED);
	}

	@Test
	@DisplayName("A partially received order stays partially received")
	void aPartiallyReceivedOrderStaysPartiallyReceived() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false);

		PurchaseOrder updated = order.afterReceiptConfirmed(false);

		assertThat(updated.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
	}

	@Test
	@DisplayName("Rejects confirming a receipt against a closed order")
	void confirmingAReceiptAgainstAClosedOrderIsRejected() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false).afterReceiptConfirmed(true);

		assertThatThrownBy(() -> order.afterReceiptConfirmed(true))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CLOSED");
	}

	@Test
	@DisplayName("An open order that does not require approval can be received against")
	void anOpenOrderNotRequiringApprovalIsReceivable() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false);

		assertThatCode(order::assertReceivable).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("A partially received order can still be received against")
	void aPartiallyReceivedOrderIsStillReceivable() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false).afterReceiptConfirmed(false);

		assertThatCode(order::assertReceivable).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("A closed order cannot be received against")
	void aClosedOrderCannotBeReceivedAgainst() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false).afterReceiptConfirmed(true);

		assertThatThrownBy(order::assertReceivable)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CLOSED");
	}

	@Test
	@DisplayName("An order still requiring approval cannot be received against")
	void anOrderRequiringApprovalCannotBeReceivedAgainst() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), true);

		assertThatThrownBy(order::assertReceivable)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("pending approval");
	}

	@Test
	@DisplayName("Approving an order pending approval clears the flag and makes it receivable")
	void approvingAnOrderPendingApprovalClearsTheFlagAndMakesItReceivable() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), true);
		UUID approvedBy = UUID.randomUUID();

		PurchaseOrder approved = order.approve(approvedBy);

		assertThat(approved.isApprovalRequired()).isFalse();
		assertThat(approved.getApprovedBy()).isEqualTo(approvedBy);
		assertThat(approved.getStatus()).isEqualTo(PurchaseOrderStatus.OPEN);
		assertThatCode(approved::assertReceivable).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Rejecting an order pending approval cancels it")
	void rejectingAnOrderPendingApprovalCancelsIt() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), true);

		PurchaseOrder rejected = order.reject();

		assertThat(rejected.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
	}

	@Test
	@DisplayName("An order that does not require approval cannot be approved")
	void anOrderNotRequiringApprovalCannotBeApproved() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), false);

		assertThatThrownBy(() -> order.approve(UUID.randomUUID()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("does not require approval");
	}

	@Test
	@DisplayName("An already approved order cannot be approved again")
	void anAlreadyApprovedOrderCannotBeApprovedAgain() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), true).approve(UUID.randomUUID());

		assertThatThrownBy(() -> order.approve(UUID.randomUUID()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("does not require approval");
	}

	@Test
	@DisplayName("A cancelled order cannot be rejected again")
	void aCancelledOrderCannotBeRejectedAgain() {
		PurchaseOrder order = create(List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00"))), true).reject();

		assertThatThrownBy(order::reject)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CANCELLED");
	}

	private PurchaseOrder create(List<PurchaseOrderItem> items, boolean approvalRequired) {
		return PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()), PurchaseRequestId.of(UUID.randomUUID()),
				null, SupplierId.of(UUID.randomUUID()), items, approvalRequired);
	}
}
