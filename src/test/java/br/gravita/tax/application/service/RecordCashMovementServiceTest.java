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

	private PosSession openSession(UUID sessionId) {
		return PosSession.of(PosSessionId.of(sessionId), UUID.randomUUID(), UUID.randomUUID(),
				CompanyId.of(UUID.randomUUID()), new BigDecimal("100.00"), PosSessionStatus.OPEN, Instant.now(), null);
	}

	@Test
	void ac1_recordsASangriaAgainstTheOpenSession() {
		UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId)))
				.thenReturn(Optional.of(openSession(sessionId)));
		when(cashMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CashMovementId id = service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA,
				new BigDecimal("50.00"), "Bank deposit"));

		ArgumentCaptor<CashMovement> captor = ArgumentCaptor.forClass(CashMovement.class);
		verify(cashMovementRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getType()).isEqualTo(CashMovementType.SANGRIA);
		assertThat(id).isEqualTo(captor.getValue().getId());
	}

	@Test
	void ac1_recordsASuprimentoAgainstTheOpenSession() {
		UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId)))
				.thenReturn(Optional.of(openSession(sessionId)));
		when(cashMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SUPRIMENTO,
				new BigDecimal("30.00"), "Change top-up"));

		ArgumentCaptor<CashMovement> captor = ArgumentCaptor.forClass(CashMovement.class);
		verify(cashMovementRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getType()).isEqualTo(CashMovementType.SUPRIMENTO);
	}

	@Test
	void ac2_blankJustificationIsRejectedBeforeTouchingTheRepository() {
		UUID sessionId = UUID.randomUUID();

		assertThatThrownBy(() -> service.execute(
				new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA, new BigDecimal("50.00"), "  ")))
				.isInstanceOf(BusinessRuleException.class);

		verify(posSessionRepositoryPort, never()).findById(any());
		verify(cashMovementRepositoryPort, never()).save(any());
	}

	@Test
	void ac3_theSavedMovementHasATimestamp() {
		UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId)))
				.thenReturn(Optional.of(openSession(sessionId)));
		when(cashMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA, new BigDecimal("50.00"),
				"Bank deposit"));

		ArgumentCaptor<CashMovement> captor = ArgumentCaptor.forClass(CashMovement.class);
		verify(cashMovementRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getTimestamp()).isNotNull();
	}

	@Test
	void ac4_theMovementIsLinkedToTheGivenSession() {
		UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId)))
				.thenReturn(Optional.of(openSession(sessionId)));
		when(cashMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA, new BigDecimal("50.00"),
				"Bank deposit"));

		ArgumentCaptor<CashMovement> captor = ArgumentCaptor.forClass(CashMovement.class);
		verify(cashMovementRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getSessionId()).isEqualTo(PosSessionId.of(sessionId));
	}

	@Test
	void ac4_recordingAgainstAClosedSessionIsRejected() {
		UUID sessionId = UUID.randomUUID();
		PosSession closedSession = PosSession.of(PosSessionId.of(sessionId), UUID.randomUUID(), UUID.randomUUID(),
				CompanyId.of(UUID.randomUUID()), new BigDecimal("100.00"), PosSessionStatus.CLOSED, Instant.now(),
				Instant.now());
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId))).thenReturn(Optional.of(closedSession));

		assertThatThrownBy(() -> service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA,
				new BigDecimal("50.00"), "Bank deposit")))
				.isInstanceOf(BusinessRuleException.class);

		verify(cashMovementRepositoryPort, never()).save(any());
	}

	@Test
	void ac4_recordingAgainstANonExistentSessionIsRejected() {
		UUID sessionId = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new RecordCashMovementCommand(sessionId, CashMovementType.SANGRIA,
				new BigDecimal("50.00"), "Bank deposit")))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(cashMovementRepositoryPort, never()).save(any());
	}
}
