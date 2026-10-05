package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.CashMovement;
import br.gravita.core.domain.tax.CashMovementId;
import br.gravita.core.domain.tax.CashMovementType;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.ports.inbound.tax.RecordCashMovementCommand;
import br.gravita.core.ports.outbound.persistence.tax.CashMovementRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.usercases.tax.RecordCashMovementService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordCashMovementServiceTest {

	@Mock
	private PosSessionRepositoryPort posSessionRepositoryPort;

	@Mock
	private CashMovementRepositoryPort cashMovementRepositoryPort;

	private RecordCashMovementService service;

	@BeforeEach
	void setUp() {
		service = new RecordCashMovementService(posSessionRepositoryPort, cashMovementRepositoryPort);
	}

	private PosSession openSession(final UUID sessionId) {
		return PosSession.of(PosSessionId.of(sessionId), UUID.randomUUID(), UUID.randomUUID(),
				CompanyId.of(UUID.randomUUID()), new BigDecimal("100.00"), PosSessionStatus.OPEN, Instant.now(), null);
	}

	@Test
	@DisplayName("Records a sangria against the open session")
	void ac1RecordsASangriaAgainstTheOpenSession() {
		final UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId)))
				.thenReturn(Optional.of(openSession(sessionId)));
		when(cashMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final CashMovementId id = service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA,
				new BigDecimal("50.00"), "Bank deposit"));

		final ArgumentCaptor<CashMovement> captor = ArgumentCaptor.forClass(CashMovement.class);
		verify(cashMovementRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getType()).isEqualTo(CashMovementType.SANGRIA);
		assertThat(id).isEqualTo(captor.getValue().getId());
	}

	@Test
	@DisplayName("Records a suprimento against the open session")
	void ac1RecordsASuprimentoAgainstTheOpenSession() {
		final UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId)))
				.thenReturn(Optional.of(openSession(sessionId)));
		when(cashMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SUPRIMENTO,
				new BigDecimal("30.00"), "Change top-up"));

		final ArgumentCaptor<CashMovement> captor = ArgumentCaptor.forClass(CashMovement.class);
		verify(cashMovementRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getType()).isEqualTo(CashMovementType.SUPRIMENTO);
	}

	@Test
	@DisplayName("Rejects a blank justification before touching the repository")
	void ac2BlankJustificationIsRejectedBeforeTouchingTheRepository() {
		final UUID sessionId = UUID.randomUUID();

		assertThatThrownBy(() -> service.execute(
				new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA, new BigDecimal("50.00"), "  ")))
				.isInstanceOf(BusinessRuleException.class);

		verify(posSessionRepositoryPort, never()).findById(any());
		verify(cashMovementRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Gives the saved movement a timestamp")
	void ac3TheSavedMovementHasATimestamp() {
		final UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId)))
				.thenReturn(Optional.of(openSession(sessionId)));
		when(cashMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA, new BigDecimal("50.00"),
				"Bank deposit"));

		final ArgumentCaptor<CashMovement> captor = ArgumentCaptor.forClass(CashMovement.class);
		verify(cashMovementRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getTimestamp()).isNotNull();
	}

	@Test
	@DisplayName("Links the movement to the given session")
	void ac4TheMovementIsLinkedToTheGivenSession() {
		final UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId)))
				.thenReturn(Optional.of(openSession(sessionId)));
		when(cashMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA, new BigDecimal("50.00"),
				"Bank deposit"));

		final ArgumentCaptor<CashMovement> captor = ArgumentCaptor.forClass(CashMovement.class);
		verify(cashMovementRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getSessionId()).isEqualTo(PosSessionId.of(sessionId));
	}

	@Test
	@DisplayName("Rejects recording a movement against a closed session")
	void ac4RecordingAgainstAClosedSessionIsRejected() {
		final UUID sessionId = UUID.randomUUID();
		final PosSession closedSession = PosSession.of(PosSessionId.of(sessionId), UUID.randomUUID(), UUID.randomUUID(),
				CompanyId.of(UUID.randomUUID()), new BigDecimal("100.00"), PosSessionStatus.CLOSED, Instant.now(),
				Instant.now());
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId))).thenReturn(Optional.of(closedSession));

		assertThatThrownBy(() -> service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA,
				new BigDecimal("50.00"), "Bank deposit")))
				.isInstanceOf(BusinessRuleException.class);

		verify(cashMovementRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects recording a movement against a non-existent session")
	void ac4RecordingAgainstANonExistentSessionIsRejected() {
		final UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA,
				new BigDecimal("50.00"), "Bank deposit")))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(cashMovementRepositoryPort, never()).save(any());
	}
}
