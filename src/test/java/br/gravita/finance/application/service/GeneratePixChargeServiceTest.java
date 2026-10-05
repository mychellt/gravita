package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GeneratePixChargeCommand;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.IssuedPixCharge;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.PixChargeIssueRequest;
import br.gravita.core.ports.outbound.persistence.finance.PixChargeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.usercases.finance.GeneratePixChargeService;
import br.gravita.finance.PixPayloads;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeneratePixChargeServiceTest {

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private PixChargeRepositoryPort pixChargeRepositoryPort;

	@Mock
	private BankIntegrationPort bankIntegrationPort;

	@InjectMocks
	private GeneratePixChargeService service;

	private final UUID customerId = UUID.randomUUID();

	private Receivable receivable(final ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), customerId, ReceivableOrigin.MANUAL,
				new BigDecimal("150.00"), LocalDate.now().plusDays(30), null, status, null, null);
	}

	@Test
	@DisplayName("Creates a pending charge with the receivable's amount and due date")
	void createsAPendingChargeMatchingTheReceivablesAmountAndDueDate() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		final Instant expiresAt = Instant.now().plus(31, ChronoUnit.DAYS);
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(bankIntegrationPort.issuePixCharge(any()))
				.thenReturn(new IssuedPixCharge(PixPayloads.valid(), expiresAt));
		when(pixChargeRepositoryPort.save(any(PixCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final PixCharge charge = service.execute(new GeneratePixChargeCommand(receivable.getId().value()));

		assertThat(charge.getReceivableId()).isEqualTo(receivable.getId());
		assertThat(charge.getDynamicQrPayload()).isEqualTo(PixPayloads.valid());
		assertThat(charge.getAmount()).isEqualByComparingTo("150.00");
		assertThat(charge.getDueDate()).isEqualTo(receivable.getDueDate());
		assertThat(charge.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(charge.getStatus()).isEqualTo(PixChargeStatus.PENDING);

		final ArgumentCaptor<PixChargeIssueRequest> request = ArgumentCaptor.forClass(PixChargeIssueRequest.class);
		verify(bankIntegrationPort).issuePixCharge(request.capture());
		assertThat(request.getValue().pixChargeId()).isEqualTo(charge.getId().value());
		assertThat(request.getValue().receivableId()).isEqualTo(receivable.getId().value());
		assertThat(request.getValue().customerId()).isEqualTo(customerId);
		assertThat(request.getValue().amount()).isEqualByComparingTo("150.00");
		assertThat(request.getValue().dueDate()).isEqualTo(receivable.getDueDate());
	}

	@Test
	@DisplayName("Rejects a receivable that is not open without contacting the bank")
	void rejectsAReceivableThatIsNotOpenWithoutContactingTheBank() {
		final Receivable settled = receivable(ReceivableStatus.SETTLED);
		when(receivableRepositoryPort.findById(settled.getId())).thenReturn(Optional.of(settled));

		assertThatThrownBy(() -> service.execute(new GeneratePixChargeCommand(settled.getId().value())))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("not OPEN");

		verifyNoInteractions(bankIntegrationPort, pixChargeRepositoryPort);
	}

	@Test
	@DisplayName("Fails when the receivable does not exist")
	void failsWhenTheReceivableDoesNotExist() {
		final UUID missing = UUID.randomUUID();
		when(receivableRepositoryPort.findById(ReceivableId.of(missing))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new GeneratePixChargeCommand(missing)))
				.isInstanceOf(ResourceNotFoundException.class);

		verifyNoInteractions(bankIntegrationPort, pixChargeRepositoryPort);
	}

	@Test
	@DisplayName("Saves nothing when the bank fails")
	void savesNothingWhenTheBankFails() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(bankIntegrationPort.issuePixCharge(any()))
				.thenThrow(new BankIntegrationUnavailableException("bank down"));

		assertThatThrownBy(() -> service.execute(new GeneratePixChargeCommand(receivable.getId().value())))
				.isInstanceOf(BankIntegrationUnavailableException.class);

		verifyNoInteractions(pixChargeRepositoryPort);
	}

	@Test
	@DisplayName("Saves nothing when the bank returns an invalid payload")
	void savesNothingWhenTheBankReturnsAnInvalidPayload() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(bankIntegrationPort.issuePixCharge(any()))
				.thenReturn(new IssuedPixCharge("garbage", Instant.now().plusSeconds(3600)));

		assertThatThrownBy(() -> service.execute(new GeneratePixChargeCommand(receivable.getId().value())))
				.isInstanceOf(BusinessRuleException.class);

		verifyNoInteractions(pixChargeRepositoryPort);
	}
}
