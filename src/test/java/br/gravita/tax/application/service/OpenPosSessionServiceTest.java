package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.ports.inbound.tax.OpenPosSessionCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.usercases.tax.OpenPosSessionService;
import java.math.BigDecimal;
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
class OpenPosSessionServiceTest {

	@Mock
	private PosSessionRepositoryPort posSessionRepositoryPort;

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	private OpenPosSessionService service;

	private CompanyId companyId;

	@BeforeEach
	void setUp() {
		service = new OpenPosSessionService(posSessionRepositoryPort, companyRepositoryPort);
		companyId = CompanyId.of(UUID.randomUUID());
		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
	}

	private Company company() {
		return Company.of(companyId, "Acme Ltda", Document.cnpj("11222333000181"), "123456789", "987654", "6201500",
				TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfce@example.com", "11999999999", null, null);
	}

	@Test
	@DisplayName("Rejects opening a session on a register that already has an open session")
	void ac1_rejectsOpeningASessionOnARegisterThatAlreadyHasAnOpenSession() {
		UUID registerId = UUID.randomUUID();
		when(posSessionRepositoryPort.existsByRegisterIdAndStatus(registerId, PosSessionStatus.OPEN))
				.thenReturn(true);

		assertThatThrownBy(() -> service.execute(
				new OpenPosSessionCommand(registerId, UUID.randomUUID(), companyId, new BigDecimal("50.00"))))
				.isInstanceOf(BusinessRuleException.class);

		verify(posSessionRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Saves the session open with its opened-at time and the given amount")
	void ac2_theSavedSessionIsOpenWithOpenedAtAndTheGivenAmount() {
		UUID registerId = UUID.randomUUID();
		when(posSessionRepositoryPort.existsByRegisterIdAndStatus(eq(registerId), any())).thenReturn(false);
		when(posSessionRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		PosSessionId id = service.execute(
				new OpenPosSessionCommand(registerId, UUID.randomUUID(), companyId, new BigDecimal("150.00")));

		ArgumentCaptor<PosSession> captor = ArgumentCaptor.forClass(PosSession.class);
		verify(posSessionRepositoryPort).save(captor.capture());
		PosSession saved = captor.getValue();
		assertThat(saved.getStatus()).isEqualTo(PosSessionStatus.OPEN);
		assertThat(saved.getOpenedAt()).isNotNull();
		assertThat(saved.getOpeningChangeAmount()).isEqualByComparingTo("150.00");
		assertThat(id).isEqualTo(saved.getId());
	}

	@Test
	@DisplayName("Links the operator to the register on the saved session")
	void ac3_theOperatorIsLinkedToTheRegisterOnTheSavedSession() {
		UUID registerId = UUID.randomUUID();
		UUID operatorId = UUID.randomUUID();
		when(posSessionRepositoryPort.existsByRegisterIdAndStatus(eq(registerId), any())).thenReturn(false);
		when(posSessionRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new OpenPosSessionCommand(registerId, operatorId, companyId, new BigDecimal("50.00")));

		ArgumentCaptor<PosSession> captor = ArgumentCaptor.forClass(PosSession.class);
		verify(posSessionRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getRegisterId()).isEqualTo(registerId);
		assertThat(captor.getValue().getOperatorId()).isEqualTo(operatorId);
	}

	@Test
	@DisplayName("Lets multiple registers each hold an independent open session")
	void ac4_multipleRegistersCanEachHoldAnIndependentOpenSession() {
		UUID registerA = UUID.randomUUID();
		UUID registerB = UUID.randomUUID();
		when(posSessionRepositoryPort.existsByRegisterIdAndStatus(any(), any())).thenReturn(false);
		when(posSessionRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		PosSessionId idA = service.execute(
				new OpenPosSessionCommand(registerA, UUID.randomUUID(), companyId, new BigDecimal("50.00")));
		PosSessionId idB = service.execute(
				new OpenPosSessionCommand(registerB, UUID.randomUUID(), companyId, new BigDecimal("75.00")));

		assertThat(idA).isNotEqualTo(idB);
		verify(posSessionRepositoryPort).existsByRegisterIdAndStatus(registerA, PosSessionStatus.OPEN);
		verify(posSessionRepositoryPort).existsByRegisterIdAndStatus(registerB, PosSessionStatus.OPEN);
	}
}
