package br.gravita.adapters.outbound.persistence.adapters.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingCommand;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingUseCase;
import br.gravita.core.ports.outbound.sales.GenerateAccountsReceivablePort.GenerateAccountsReceivableCommand;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateAccountsReceivableAdapterTest {

	@Mock
	private GenerateReceivableFromInvoicingUseCase useCase;

	@InjectMocks
	private GenerateAccountsReceivableAdapter adapter;

	private final UUID customerId = UUID.randomUUID();
	private final FiscalDocumentRef document = new FiscalDocumentRef(FiscalDocumentType.NFE, UUID.randomUUID());
	private final GenerateAccountsReceivableCommand command = new GenerateAccountsReceivableCommand(customerId,
			document, new BigDecimal("80.00"), LocalDate.of(2026, 10, 30));

	@Test
	@DisplayName("Delegates as a single installment that references the fiscal document")
	void delegatesAsASingleInstallmentReferencingTheFiscalDocument() {
		adapter.generate(command);

		final ArgumentCaptor<GenerateReceivableFromInvoicingCommand> captured = ArgumentCaptor
				.forClass(GenerateReceivableFromInvoicingCommand.class);
		verify(useCase).execute(captured.capture());
		assertThat(captured.getValue().customerId()).isEqualTo(customerId);
		assertThat(captured.getValue().originDocumentRef()).isEqualTo(document.documentId());
		assertThat(captured.getValue().installments()).singleElement().satisfies(installment -> {
			assertThat(installment.dueDate()).isEqualTo(LocalDate.of(2026, 10, 30));
			assertThat(installment.amount()).isEqualByComparingTo("80.00");
		});
	}

	@Test
	@DisplayName("Swallows failures so they never break the invoicing flow")
	void failureNeverPropagatesToTheInvoicingFlow() {
		when(useCase.execute(any())).thenThrow(new IllegalStateException("db down"));

		assertThatCode(() -> adapter.generate(command)).doesNotThrowAnyException();
	}
}
