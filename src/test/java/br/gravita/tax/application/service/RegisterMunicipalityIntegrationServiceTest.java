package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import br.gravita.core.domain.tax.NfseStandard;
import br.gravita.core.ports.inbound.tax.RegisterMunicipalityIntegrationCommand;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalityIntegrationRepositoryPort;
import br.gravita.core.usercases.tax.RegisterMunicipalityIntegrationService;
import java.util.List;
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
class RegisterMunicipalityIntegrationServiceTest {

	@Mock
	private MunicipalityIntegrationRepositoryPort repositoryPort;

	private RegisterMunicipalityIntegrationService service;

	@BeforeEach
	void setUp() {
		service = new RegisterMunicipalityIntegrationService(repositoryPort);
		org.mockito.Mockito.lenient().when(repositoryPort.save(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	private static RegisterMunicipalityIntegrationCommand command(NfseStandard standard, boolean homologated) {
		return new RegisterMunicipalityIntegrationCommand("3550308", standard, "2.04", "https://nfse.example/ws",
				CertificateType.A1, List.of("inscricaoMunicipal"), homologated);
	}

	@Test
	@DisplayName("Registers a new municipality for each supported standard")
	void ac1_registersANewMunicipalityForEachStandard() {
		for (NfseStandard standard : NfseStandard.values()) {
			when(repositoryPort.findByIbgeCode("3550308")).thenReturn(Optional.empty());

			MunicipalityIntegrationId id = service.execute(command(standard, true));

			assertThat(id).isNotNull();
		}
		ArgumentCaptor<MunicipalityIntegration> saved = ArgumentCaptor.forClass(MunicipalityIntegration.class);
		verify(repositoryPort, org.mockito.Mockito.times(NfseStandard.values().length)).save(saved.capture());
		assertThat(saved.getAllValues()).extracting(MunicipalityIntegration::getStandard)
				.containsExactly(NfseStandard.values());
	}

	@Test
	@DisplayName("Accepts a non-homologated registration")
	void ac2_nonHomologatedRegistrationIsAccepted() {
		when(repositoryPort.findByIbgeCode("3550308")).thenReturn(Optional.empty());

		service.execute(new RegisterMunicipalityIntegrationCommand("3550308", NfseStandard.BETHA, null, null,
				CertificateType.A3, null, false));

		ArgumentCaptor<MunicipalityIntegration> saved = ArgumentCaptor.forClass(MunicipalityIntegration.class);
		verify(repositoryPort).save(saved.capture());
		assertThat(saved.getValue().isHomologated()).isFalse();
	}

	@Test
	@DisplayName("Updates the existing configuration on re-registration instead of creating another")
	void ac3_reRegisteringUpdatesTheExistingConfigurationInsteadOfCreatingAnother() {
		MunicipalityIntegration existing = MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()),
				"3550308", NfseStandard.ABRASF, "2.03", "https://old", CertificateType.A1, List.of(), false);
		when(repositoryPort.findByIbgeCode("3550308")).thenReturn(Optional.of(existing));

		MunicipalityIntegrationId id = service.execute(command(NfseStandard.ISSNET, true));

		assertThat(id).isEqualTo(existing.getId());
		ArgumentCaptor<MunicipalityIntegration> saved = ArgumentCaptor.forClass(MunicipalityIntegration.class);
		verify(repositoryPort).save(saved.capture());
		assertThat(saved.getValue()).isSameAs(existing);
		assertThat(existing.getStandard()).isEqualTo(NfseStandard.ISSNET);
		assertThat(existing.getWebserviceUrl()).isEqualTo("https://nfse.example/ws");
		assertThat(existing.isHomologated()).isTrue();
	}

	@Test
	@DisplayName("Registers a municipality without depending on any NFS-e adapter")
	void ac4_registrationDoesNotDependOnAnyNfseAdapter() {
		// The service's only collaborator is the repository: there is nothing to fail when no adapter is deployed.
		when(repositoryPort.findByIbgeCode("3550308")).thenReturn(Optional.empty());

		assertThat(service.execute(command(NfseStandard.NFSE_NACIONAL, true))).isNotNull();
	}
}
