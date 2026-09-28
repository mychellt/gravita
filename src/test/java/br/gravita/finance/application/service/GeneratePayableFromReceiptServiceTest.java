package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptCommand;
import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptCommand.Installment;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.usercases.finance.GeneratePayableFromReceiptService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeneratePayableFromReceiptServiceTest {

	private static final LocalDate FIRST_DUE = LocalDate.of(2026, 10, 30);

	@Mock
	private PayableRepositoryPort payableRepositoryPort;

	@InjectMocks
	private GeneratePayableFromReceiptService service;

	@Test
	void createsOneOpenPurchaseReceiptPayablePerInstallmentReferencingTheReceipt() {
		UUID supplierId = UUID.randomUUID();
		UUID receiptId = UUID.randomUUID();
		when(payableRepositoryPort.findByPurchaseReceiptRef(receiptId)).thenReturn(List.of());
		when(payableRepositoryPort.save(any(Payable.class))).thenAnswer(invocation -> invocation.getArgument(0));

		List<Payable> created = service.execute(new GeneratePayableFromReceiptCommand(supplierId, receiptId,
				List.of(new Installment(FIRST_DUE, new BigDecimal("100.00")),
						new Installment(FIRST_DUE.plusDays(30), new BigDecimal("100.00")),
						new Installment(FIRST_DUE.plusDays(60), new BigDecimal("50.00")))));

		assertThat(created).hasSize(3);
		assertThat(created).allSatisfy(payable -> {
			assertThat(payable.getOrigin()).isEqualTo(PayableOrigin.PURCHASE_RECEIPT);
			assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
			assertThat(payable.getSupplierId()).isEqualTo(supplierId);
			assertThat(payable.getPurchaseReceiptRef()).isEqualTo(receiptId);
			assertThat(payable.getInstallments()).isEqualTo(3);
		});
		assertThat(created).extracting(Payable::getInstallmentNumber).containsExactly(1, 2, 3);
		assertThat(created).extracting(Payable::getDueDate).containsExactly(FIRST_DUE, FIRST_DUE.plusDays(30),
				FIRST_DUE.plusDays(60));
		assertThat(created).extracting(Payable::getAmount)
				.usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
				.containsExactly(new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("50.00"));
		verify(payableRepositoryPort, times(3)).save(any(Payable.class));
	}

	@Test
	void isIdempotentPerPurchaseReceiptAndReturnsTheExistingPayables() {
		UUID supplierId = UUID.randomUUID();
		UUID receiptId = UUID.randomUUID();
		Payable existing = Payable.createFromPurchaseReceipt(PayableId.of(UUID.randomUUID()), supplierId, receiptId,
				BigDecimal.TEN, FIRST_DUE, 1, 1);
		when(payableRepositoryPort.findByPurchaseReceiptRef(receiptId)).thenReturn(List.of(existing));

		List<Payable> result = service.execute(new GeneratePayableFromReceiptCommand(supplierId, receiptId,
				List.of(new Installment(FIRST_DUE, BigDecimal.TEN))));

		assertThat(result).containsExactly(existing);
		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsACommandWithoutInstallmentsWithoutSavingAnything() {
		UUID receiptId = UUID.randomUUID();

		assertThatThrownBy(() -> service
				.execute(new GeneratePayableFromReceiptCommand(UUID.randomUUID(), receiptId, List.of())))
				.isInstanceOf(BusinessRuleException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsANonPositiveInstallmentAmount() {
		UUID receiptId = UUID.randomUUID();
		when(payableRepositoryPort.findByPurchaseReceiptRef(receiptId)).thenReturn(List.of());

		assertThatThrownBy(() -> service.execute(new GeneratePayableFromReceiptCommand(UUID.randomUUID(),
				receiptId, List.of(new Installment(FIRST_DUE, BigDecimal.ZERO)))))
				.isInstanceOf(BusinessRuleException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void savesEachPayableWithItsOwnId() {
		UUID receiptId = UUID.randomUUID();
		when(payableRepositoryPort.findByPurchaseReceiptRef(receiptId)).thenReturn(List.of());
		when(payableRepositoryPort.save(any(Payable.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new GeneratePayableFromReceiptCommand(UUID.randomUUID(), receiptId,
				List.of(new Installment(FIRST_DUE, BigDecimal.ONE), new Installment(FIRST_DUE, BigDecimal.ONE))));

		ArgumentCaptor<Payable> saved = ArgumentCaptor.forClass(Payable.class);
		verify(payableRepositoryPort, times(2)).save(saved.capture());
		assertThat(saved.getAllValues()).extracting(Payable::getId).doesNotHaveDuplicates();
	}
}
