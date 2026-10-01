package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.ConfirmBatchPaymentCommand;
import br.gravita.core.ports.inbound.finance.ConfirmBatchPaymentUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.usercases.finance.ConfirmDailyBatchPaymentService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmDailyBatchPaymentServiceTest {

	@Mock
	private BankIntegrationPort bankIntegrationPort;

	@Mock
	private ConfirmBatchPaymentUseCase confirmBatchPaymentUseCase;

	@InjectMocks
	private ConfirmDailyBatchPaymentService service;

	@Test
	@DisplayName("Confirms the payments from the file the bank published")
	void confirmsTheFileTheBankPublished() {
		BankReturnImportResult expected = new BankReturnImportResult(2, 0, List.of());
		when(bankIntegrationPort.fetchPaymentReturnFile(BankIntegration.SICOOB)).thenReturn(Optional.of("cnab"));
		when(confirmBatchPaymentUseCase.execute(new ConfirmBatchPaymentCommand(BankIntegration.SICOOB, "cnab")))
				.thenReturn(expected);

		assertThat(service.execute(BankIntegration.SICOOB)).contains(expected);
	}

	@Test
	@DisplayName("Does nothing when the bank has no file for today")
	void doesNothingWhenTheBankHasNoFileToday() {
		when(bankIntegrationPort.fetchPaymentReturnFile(BankIntegration.SICOOB)).thenReturn(Optional.empty());

		assertThat(service.execute(BankIntegration.SICOOB)).isEmpty();
		verify(confirmBatchPaymentUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Treats a blank file as if there were no file")
	void treatsABlankFileAsNoFile() {
		when(bankIntegrationPort.fetchPaymentReturnFile(BankIntegration.SICOOB)).thenReturn(Optional.of("  \n"));

		assertThat(service.execute(BankIntegration.SICOOB)).isEmpty();
		verify(confirmBatchPaymentUseCase, never()).execute(any());
	}
}
