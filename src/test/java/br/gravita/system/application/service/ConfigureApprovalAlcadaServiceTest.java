package br.gravita.system.application.service;

import br.gravita.shared.BusinessRuleException;
import br.gravita.system.application.port.in.ConfigureApprovalAlcadaCommand;
import br.gravita.system.application.port.out.ApprovalAlcadaRepositoryPort;
import br.gravita.system.application.port.out.ProfileRepositoryPort;
import br.gravita.system.domain.model.ApprovalAlcada;
import br.gravita.system.domain.model.ApprovalModule;
import br.gravita.system.domain.model.ProfileReference;
import br.gravita.system.domain.model.UnknownApprovalModuleException;
import br.gravita.system.domain.model.UnknownProfileException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigureApprovalAlcadaServiceTest {

	@Mock
	private ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	private static final ProfileReference APPROVER = new ProfileReference(UUID.randomUUID(), "Financial Manager");

	@Test
	void shouldCreateNewAlcadaWhenModuleHasNoneConfiguredYet() {
		ConfigureApprovalAlcadaService service =
				new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryPort, profileRepositoryPort);
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.empty());
		when(profileRepositoryPort.findById(APPROVER.id())).thenReturn(Optional.of(APPROVER));

		service.execute(new ConfigureApprovalAlcadaCommand("purchasing", new BigDecimal("5000.00"), null,
				APPROVER.id()));

		ArgumentCaptor<ApprovalAlcada> captor = ArgumentCaptor.forClass(ApprovalAlcada.class);
		verify(approvalAlcadaRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getModule()).isEqualTo(ApprovalModule.PURCHASING);
		assertThat(captor.getValue().getThresholdValue()).isEqualByComparingTo("5000.00");
		assertThat(captor.getValue().getApproverProfileId()).isEqualTo(APPROVER.id());
	}

	@Test
	void shouldReconfigureExistingAlcadaInsteadOfCreatingASecondOne() {
		ConfigureApprovalAlcadaService service =
				new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryPort, profileRepositoryPort);
		ApprovalAlcada existing = ApprovalAlcada.configure(ApprovalModule.SALES, null, new BigDecimal("10.00"),
				APPROVER);
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.SALES)).thenReturn(Optional.of(existing));
		when(profileRepositoryPort.findById(APPROVER.id())).thenReturn(Optional.of(APPROVER));

		service.execute(new ConfigureApprovalAlcadaCommand("sales", null, new BigDecimal("20.00"), APPROVER.id()));

		ArgumentCaptor<ApprovalAlcada> captor = ArgumentCaptor.forClass(ApprovalAlcada.class);
		verify(approvalAlcadaRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(existing.getId());
		assertThat(captor.getValue().getThresholdDiscountPercent()).isEqualByComparingTo("20.00");
	}

	@Test
	void shouldRejectUnknownModuleBeforeTouchingAnyRepository() {
		ConfigureApprovalAlcadaService service =
				new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryPort, profileRepositoryPort);

		assertThatThrownBy(() -> service.execute(
				new ConfigureApprovalAlcadaCommand("logistics", new BigDecimal("100"), null, APPROVER.id())))
				.isInstanceOf(UnknownApprovalModuleException.class);

		verify(profileRepositoryPort, never()).findById(any());
		verify(approvalAlcadaRepositoryPort, never()).save(any());
	}

	@Test
	void shouldRejectUnknownApproverProfile() {
		ConfigureApprovalAlcadaService service =
				new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryPort, profileRepositoryPort);
		UUID unknownProfileId = UUID.randomUUID();
		when(profileRepositoryPort.findById(unknownProfileId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new ConfigureApprovalAlcadaCommand("finance", new BigDecimal("100"), null, unknownProfileId)))
				.isInstanceOf(UnknownProfileException.class);

		verify(approvalAlcadaRepositoryPort, never()).findByModule(any());
		verify(approvalAlcadaRepositoryPort, never()).save(any());
	}

	@Test
	void shouldRejectCommandWithoutAnyThreshold() {
		ConfigureApprovalAlcadaService service =
				new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryPort, profileRepositoryPort);
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.FINANCE)).thenReturn(Optional.empty());
		when(profileRepositoryPort.findById(APPROVER.id())).thenReturn(Optional.of(APPROVER));

		assertThatThrownBy(() -> service.execute(
				new ConfigureApprovalAlcadaCommand("finance", null, null, APPROVER.id())))
				.isInstanceOf(BusinessRuleException.class);

		verify(approvalAlcadaRepositoryPort, never()).save(any());
	}
}
