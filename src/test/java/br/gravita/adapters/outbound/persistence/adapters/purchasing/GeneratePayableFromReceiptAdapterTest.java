package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptCommand;
import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort;
import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort.GeneratePayableFromReceiptCommand.Installment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeneratePayableFromReceiptAdapterTest {

	@Mock
	private GeneratePayableFromReceiptUseCase useCase;

	@InjectMocks
	private GeneratePayableFromReceiptAdapter adapter;

	private final UUID supplierId = UUID.randomUUID();
	private final UUID receiptId = UUID.randomUUID();
	private final GeneratePayableFromReceiptPort.GeneratePayableFromReceiptCommand command = new GeneratePayableFromReceiptPort.GeneratePayableFromReceiptCommand(
			receiptId, supplierId,
			List.of(new Installment(new BigDecimal("80.00"), LocalDate.of(2026, 10, 30)),
					new Installment(new BigDecimal("20.00"), LocalDate.of(2026, 11, 30))));

	@Test
	@DisplayName("Delegates the receipt's installments in order")
	void delegatesTheReceiptsInstallmentsInOrder() {
		adapter.generatePayables(command);

		final ArgumentCaptor<GeneratePayableFromReceiptCommand> captured = ArgumentCaptor
				.forClass(GeneratePayableFromReceiptCommand.class);
		verify(useCase).execute(captured.capture());
		assertThat(captured.getValue().supplierId()).isEqualTo(supplierId);
		assertThat(captured.getValue().purchaseReceiptRef()).isEqualTo(receiptId);
		assertThat(captured.getValue().installments()).satisfiesExactly(first -> {
			assertThat(first.amount()).isEqualByComparingTo("80.00");
			assertThat(first.dueDate()).isEqualTo(LocalDate.of(2026, 10, 30));
		}, second -> {
			assertThat(second.amount()).isEqualByComparingTo("20.00");
			assertThat(second.dueDate()).isEqualTo(LocalDate.of(2026, 11, 30));
		});
	}

	@Test
	@DisplayName("Propagates a failure, leaving the receipt unconfirmed instead of swallowing it")
	void failureLeavesTheReceiptUnconfirmedInsteadOfBeingSwallowed() {
		when(useCase.execute(any())).thenThrow(new IllegalStateException("db down"));

		assertThatThrownBy(() -> adapter.generatePayables(command)).isInstanceOf(IllegalStateException.class);
	}
}
