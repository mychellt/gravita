package br.gravita.system.application.service;

import br.gravita.core.usercases.tax.ConfigureApprovalAlcadaService;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.usercases.system.ConfigureApprovalAlcadaCommand;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.UnknownApprovalModuleException;
import br.gravita.core.domain.system.UnknownProfileException;
import org.junit.jupiter.api.DisplayName;
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
	@DisplayName("Creates a new alcada when the module has none configured yet")
	void shouldCreateNewAlcadaWhenModuleHasNoneConfiguredYet() {
		final ConfigureApprovalAlcadaService service =
				new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryPort, profileRepositoryPort);
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.empty());
		when(profileRepositoryPort.findById(APPROVER.id())).thenReturn(Optional.of(APPROVER));

		service.execute(new ConfigureApprovalAlcadaCommand("purchasing", new BigDecimal("5000.00"), null,
				APPROVER.id()));

		final ArgumentCaptor<ApprovalAlcada> captor = ArgumentCaptor.forClass(ApprovalAlcada.class);
		verify(approvalAlcadaRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getModule()).isEqualTo(ApprovalModule.PURCHASING);
		assertThat(captor.getValue().getThresholdValue()).isEqualByComparingTo("5000.00");
		assertThat(captor.getValue().getApproverProfileId()).isEqualTo(APPROVER.id());
	}

	@Test
	@DisplayName("Reconfigures the existing alcada instead of creating a second one")
	void shouldReconfigureExistingAlcadaInsteadOfCreatingASecondOne() {
		final ConfigureApprovalAlcadaService service =
				new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryPort, profileRepositoryPort);
		final ApprovalAlcada existing = ApprovalAlcada.configure(ApprovalModule.SALES, null, new BigDecimal("10.00"),
				APPROVER);
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.SALES)).thenReturn(Optional.of(existing));
		when(profileRepositoryPort.findById(APPROVER.id())).thenReturn(Optional.of(APPROVER));

		service.execute(new ConfigureApprovalAlcadaCommand("sales", null, new BigDecimal("20.00"), APPROVER.id()));

		final ArgumentCaptor<ApprovalAlcada> captor = ArgumentCaptor.forClass(ApprovalAlcada.class);
		verify(approvalAlcadaRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(existing.getId());
		assertThat(captor.getValue().getThresholdDiscountPercent()).isEqualByComparingTo("20.00");
	}

	@Test
	@DisplayName("Rejects an unknown module before touching any repository")
	void shouldRejectUnknownModuleBeforeTouchingAnyRepository() {
		final ConfigureApprovalAlcadaService service =
				new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryPort, profileRepositoryPort);

		assertThatThrownBy(() -> service.execute(
				new ConfigureApprovalAlcadaCommand("logistics", new BigDecimal("100"), null, APPROVER.id())))
				.isInstanceOf(UnknownApprovalModuleException.class);

		verify(profileRepositoryPort, never()).findById(any());
		verify(approvalAlcadaRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects an unknown approver profile")
	void shouldRejectUnknownApproverProfile() {
		final ConfigureApprovalAlcadaService service =
				new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryPort, profileRepositoryPort);
		final UUID unknownProfileId = UUID.randomUUID();
		when(profileRepositoryPort.findById(unknownProfileId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new ConfigureApprovalAlcadaCommand("finance", new BigDecimal("100"), null, unknownProfileId)))
				.isInstanceOf(UnknownProfileException.class);

		verify(approvalAlcadaRepositoryPort, never()).findByModule(any());
		verify(approvalAlcadaRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a command that has no threshold")
	void shouldRejectCommandWithoutAnyThreshold() {
		final ConfigureApprovalAlcadaService service =
				new ConfigureApprovalAlcadaService(approvalAlcadaRepositoryPort, profileRepositoryPort);
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.FINANCE)).thenReturn(Optional.empty());
		when(profileRepositoryPort.findById(APPROVER.id())).thenReturn(Optional.of(APPROVER));

		assertThatThrownBy(() -> service.execute(
				new ConfigureApprovalAlcadaCommand("finance", null, null, APPROVER.id())))
				.isInstanceOf(BusinessRuleException.class);

		verify(approvalAlcadaRepositoryPort, never()).save(any());
	}
}
