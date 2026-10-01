package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.ImportBankReturnCommand;
import br.gravita.core.ports.inbound.finance.ImportBankReturnUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.usercases.finance.ImportDailyBankReturnService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportDailyBankReturnServiceTest {

	@Mock
	private BankIntegrationPort bankIntegrationPort;

	@Mock
	private ImportBankReturnUseCase importBankReturnUseCase;

	@InjectMocks
	private ImportDailyBankReturnService service;

	@Test
	@DisplayName("Imports the file the bank published")
	void importsTheFileTheBankPublished() {
		BankReturnImportResult expected = new BankReturnImportResult(2, 0, List.of());
		when(bankIntegrationPort.fetchReturnFile(BankIntegration.SICOOB)).thenReturn(Optional.of("cnab"));
		when(importBankReturnUseCase.execute(new ImportBankReturnCommand(BankIntegration.SICOOB, "cnab")))
				.thenReturn(expected);

		assertThat(service.execute(BankIntegration.SICOOB)).contains(expected);
	}

	@Test
	@DisplayName("Does nothing when the bank has no file for today")
	void doesNothingWhenTheBankHasNoFileToday() {
		when(bankIntegrationPort.fetchReturnFile(BankIntegration.SICOOB)).thenReturn(Optional.empty());

		assertThat(service.execute(BankIntegration.SICOOB)).isEmpty();
		verify(importBankReturnUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Treats a blank file as if there were no file")
	void treatsABlankFileAsNoFile() {
		when(bankIntegrationPort.fetchReturnFile(BankIntegration.SICOOB)).thenReturn(Optional.of("  \n"));

		assertThat(service.execute(BankIntegration.SICOOB)).isEmpty();
		verify(importBankReturnUseCase, never()).execute(any());
	}
}
