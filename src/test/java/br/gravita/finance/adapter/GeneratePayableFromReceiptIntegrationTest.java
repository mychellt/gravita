package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptCommand;
import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptCommand.Installment;
import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptUseCase;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GeneratePayableFromReceiptIntegrationTest {

	@Autowired
	private GeneratePayableFromReceiptUseCase useCase;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@Test
	@DisplayName("Persists one payable per installment and does not duplicate them when the receipt is reconfirmed")
	void persistsOnePayablePerInstallmentAndDoesNotDuplicateOnReconfirmation() {
		UUID supplierId = UUID.randomUUID();
		UUID receiptId = UUID.randomUUID();
		GeneratePayableFromReceiptCommand command = new GeneratePayableFromReceiptCommand(supplierId, receiptId,
				List.of(new Installment(LocalDate.now().plusDays(30), new BigDecimal("60.00")),
						new Installment(LocalDate.now().plusDays(60), new BigDecimal("40.00"))));

		List<Payable> first = useCase.execute(command);
		List<Payable> second = useCase.execute(command);

		assertThat(first).hasSize(2);
		assertThat(second).extracting(Payable::getId)
				.containsExactlyElementsOf(first.stream().map(Payable::getId).toList());
		List<Payable> stored = payableRepositoryPort.findByPurchaseReceiptRef(receiptId);
		assertThat(stored).hasSize(2);
		assertThat(stored).allSatisfy(payable -> {
			assertThat(payable.getOrigin()).isEqualTo(PayableOrigin.PURCHASE_RECEIPT);
			assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
			assertThat(payable.getSupplierId()).isEqualTo(supplierId);
			assertThat(payable.getInstallments()).isEqualTo(2);
		});
		assertThat(stored).extracting(Payable::getInstallmentNumber).containsExactly(1, 2);
		assertThat(stored).extracting(Payable::getAmount)
				.usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
				.containsExactly(new BigDecimal("60.00"), new BigDecimal("40.00"));
	}
}
