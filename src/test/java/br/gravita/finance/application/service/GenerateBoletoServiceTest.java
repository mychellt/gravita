package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.domain.finance.Boleto;
import br.gravita.core.domain.finance.BoletoStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GenerateBoletoCommand;
import br.gravita.core.ports.messaging.EmailNotificationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.BoletoIssueRequest;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.IssuedBoleto;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.BoletoRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.usercases.finance.GenerateBoletoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateBoletoServiceTest {

	private static final String BARCODE_LINE = "00190500954014481606906809350314437370000000100";

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private BoletoRepositoryPort boletoRepositoryPort;

	@Mock
	private BankIntegrationPort bankIntegrationPort;

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private EmailNotificationPort emailNotificationPort;

	@InjectMocks
	private GenerateBoletoService service;

	private final UUID customerId = UUID.randomUUID();

	private Receivable receivable(ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), customerId, ReceivableOrigin.MANUAL,
				new BigDecimal("150.00"), LocalDate.now().plusDays(30), null, status, null, null);
	}

	@Test
	void generatesALinkedBoletoFromTheChosenBankAndEmailsIt() {
		Receivable receivable = receivable(ReceivableStatus.OPEN);
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(bankIntegrationPort.issueBoleto(any())).thenReturn(new IssuedBoleto(BARCODE_LINE));
		when(boletoRepositoryPort.save(any(Boleto.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(customerRepositoryPort.get(customerId))
				.thenReturn(Optional.of(CustomerDomain.builder().email("cliente@example.com").build()));

		Boleto boleto = service
				.execute(new GenerateBoletoCommand(receivable.getId().value(), BankIntegration.SICOOB));

		assertThat(boleto.getReceivableId()).isEqualTo(receivable.getId());
		assertThat(boleto.getBankIntegration()).isEqualTo(BankIntegration.SICOOB);
		assertThat(boleto.getBarcodeLine()).isEqualTo(BARCODE_LINE);
		assertThat(boleto.getStatus()).isEqualTo(BoletoStatus.ISSUED);

		ArgumentCaptor<BoletoIssueRequest> request = ArgumentCaptor.forClass(BoletoIssueRequest.class);
		verify(bankIntegrationPort).issueBoleto(request.capture());
		assertThat(request.getValue().bankIntegration()).isEqualTo(BankIntegration.SICOOB);
		assertThat(request.getValue().amount()).isEqualByComparingTo("150.00");
		assertThat(request.getValue().dueDate()).isEqualTo(receivable.getDueDate());

		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(emailNotificationPort).send(org.mockito.ArgumentMatchers.eq("cliente@example.com"), anyString(),
				body.capture());
		assertThat(body.getValue()).contains(BARCODE_LINE);
	}

	@Test
	void rejectsAReceivableThatIsNotOpenWithoutContactingTheBank() {
		Receivable settled = receivable(ReceivableStatus.SETTLED);
		when(receivableRepositoryPort.findById(settled.getId())).thenReturn(Optional.of(settled));

		assertThatThrownBy(
				() -> service.execute(new GenerateBoletoCommand(settled.getId().value(), BankIntegration.ITAU)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("not OPEN");

		verifyNoInteractions(bankIntegrationPort, boletoRepositoryPort, emailNotificationPort);
	}

	@Test
	void failsWhenTheReceivableDoesNotExist() {
		UUID missing = UUID.randomUUID();
		when(receivableRepositoryPort.findById(ReceivableId.of(missing))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new GenerateBoletoCommand(missing, BankIntegration.ITAU)))
				.isInstanceOf(ResourceNotFoundException.class);

		verifyNoInteractions(bankIntegrationPort, boletoRepositoryPort, emailNotificationPort);
	}

	@Test
	void savesNothingAndSendsNothingWhenTheBankFails() {
		Receivable receivable = receivable(ReceivableStatus.OPEN);
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(bankIntegrationPort.issueBoleto(any()))
				.thenThrow(new BankIntegrationUnavailableException("bank down"));

		assertThatThrownBy(() -> service
				.execute(new GenerateBoletoCommand(receivable.getId().value(), BankIntegration.BRADESCO)))
				.isInstanceOf(BankIntegrationUnavailableException.class);

		verifyNoInteractions(boletoRepositoryPort, emailNotificationPort);
	}

	@Test
	void stillGeneratesTheBoletoWhenTheCustomerHasNoEmail() {
		Receivable receivable = receivable(ReceivableStatus.OPEN);
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(bankIntegrationPort.issueBoleto(any())).thenReturn(new IssuedBoleto(BARCODE_LINE));
		when(boletoRepositoryPort.save(any(Boleto.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(CustomerDomain.builder().build()));

		Boleto boleto = service
				.execute(new GenerateBoletoCommand(receivable.getId().value(), BankIntegration.SICREDI));

		assertThat(boleto.getBarcodeLine()).isEqualTo(BARCODE_LINE);
		verify(emailNotificationPort, never()).send(anyString(), anyString(), anyString());
	}
}
