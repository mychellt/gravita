package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayableTest {

	private static final LocalDate DUE = LocalDate.now().plusDays(10);

	private static PayableId id() {
		return PayableId.of(UUID.randomUUID());
	}

	private static CostCenterShare share(UUID costCenterId, String percent) {
		return new CostCenterShare(costCenterId, new BigDecimal(percent));
	}

	@Test
	void aPurchaseReceiptPayableStartsOpenReferencingTheReceiptAndItsInstallment() {
		UUID supplierId = UUID.randomUUID();
		UUID receiptId = UUID.randomUUID();

		Payable payable = Payable.createFromPurchaseReceipt(id(), supplierId, receiptId, new BigDecimal("450.00"),
				DUE, 2, 3);

		assertThat(payable.getOrigin()).isEqualTo(PayableOrigin.PURCHASE_RECEIPT);
		assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
		assertThat(payable.getSupplierId()).isEqualTo(supplierId);
		assertThat(payable.getPurchaseReceiptRef()).isEqualTo(receiptId);
		assertThat(payable.getInstallmentNumber()).isEqualTo(2);
		assertThat(payable.getInstallments()).isEqualTo(3);
		assertThat(payable.getAmount()).isEqualByComparingTo("450.00");
		assertThat(payable.getDueDate()).isEqualTo(DUE);
		assertThat(payable.getCostCenterSplit()).isEmpty();
	}

	@Test
	void aPurchaseReceiptPayableRequiresSupplierAndReceipt() {
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), null, UUID.randomUUID(), BigDecimal.TEN,
				DUE, 1, 1)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), null, BigDecimal.TEN,
				DUE, 1, 1)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void aPurchaseReceiptPayableRejectsAnInstallmentNumberOutsideTheTotal() {
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), UUID.randomUUID(),
				BigDecimal.TEN, DUE, 0, 2)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), UUID.randomUUID(),
				BigDecimal.TEN, DUE, 3, 2)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aPurchaseReceiptPayableRejectsANonPositiveAmount() {
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), UUID.randomUUID(),
				BigDecimal.ZERO, DUE, 1, 1)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aManualPayableHasNoReceiptReference() {
		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);

		assertThat(payable.getPurchaseReceiptRef()).isNull();
		assertThat(payable.getInstallmentNumber()).isNull();
		assertThat(payable.getInstallments()).isNull();
	}

	@Test
	void aManualPayableStartsOpenWithManualOrigin() {
		UUID supplierId = UUID.randomUUID();

		Payable payable = Payable.createManual(id(), supplierId, new BigDecimal("1200.00"), DUE, null);

		assertThat(payable.getOrigin()).isEqualTo(PayableOrigin.MANUAL);
		assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
		assertThat(payable.getSupplierId()).isEqualTo(supplierId);
		assertThat(payable.getAmount()).isEqualByComparingTo("1200.00");
		assertThat(payable.getDueDate()).isEqualTo(DUE);
		assertThat(payable.getCostCenterSplit()).isEmpty();
	}

	@Test
	void theSupplierIsOptionalForPureExpenses() {
		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, List.of());

		assertThat(payable.getSupplierId()).isNull();
	}

	@Test
	void amountIsRequired() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, null, DUE, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void amountMustBePositive() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.ZERO, DUE, null))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> Payable.createManual(id(), null, new BigDecimal("-1"), DUE, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void dueDateIsRequired() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.TEN, null, null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("dueDate");
	}

	@Test
	void aCostCenterSplitSummingTo100IsKept() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();

		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE,
				List.of(share(a, "60.00"), share(b, "40.00")));

		assertThat(payable.getCostCenterSplit()).extracting(CostCenterShare::costCenterId).containsExactly(a, b);
	}

	@Test
	void aCostCenterSplitNotSummingTo100IsRejected() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.TEN, DUE,
				List.of(share(UUID.randomUUID(), "60"), share(UUID.randomUUID(), "30"))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("sum to 100");
	}

	@Test
	void aCostCenterCannotAppearTwiceInTheSplit() {
		UUID a = UUID.randomUUID();

		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.TEN, DUE,
				List.of(share(a, "50"), share(a, "50")))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aSharePercentMustBeBetweenZeroExclusiveAndOneHundred() {
		UUID a = UUID.randomUUID();

		assertThatThrownBy(() -> share(a, "0")).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> share(a, "100.01")).isInstanceOf(BusinessRuleException.class);
		assertThat(share(a, "100").percent()).isEqualByComparingTo("100");
	}

	@Test
	void shareOfAppliesTheCostCentersPercentageAndIsZeroForACostCenterNotCharged() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		Payable payable = Payable.createManual(id(), null, new BigDecimal("100.00"), DUE,
				List.of(share(a, "33.33"), share(b, "66.67")));

		assertThat(payable.shareOf(new BigDecimal("100.00"), a)).isEqualByComparingTo("33.33");
		assertThat(payable.shareOf(new BigDecimal("50.00"), b)).isEqualByComparingTo("33.34");
		assertThat(payable.shareOf(new BigDecimal("100.00"), UUID.randomUUID())).isEqualByComparingTo("0");
		assertThat(payable.shareOf(new BigDecimal("100.00"), null)).isEqualByComparingTo("100.00");
	}

	@Test
	void isOutstandingWhileOpenOrApproved() {
		for (PayableStatus status : PayableStatus.values()) {
			Payable payable = Payable.of(id(), null, PayableOrigin.MANUAL, BigDecimal.TEN, DUE, List.of(), status,
					null, null, null);

			assertThat(payable.isOutstanding())
					.isEqualTo(status == PayableStatus.OPEN || status == PayableStatus.APPROVED);
		}
	}

	@Test
	void aPayableIsInNoScopeUntilPlacedInOne() {
		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);
		LedgerScope scope = new LedgerScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		assertThat(payable.getScope()).isEqualTo(LedgerScope.NONE);
		assertThat(payable.inScope(scope).getScope()).isEqualTo(scope);
	}
}
