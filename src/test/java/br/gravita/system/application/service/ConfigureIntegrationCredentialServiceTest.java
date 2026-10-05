package br.gravita.system.application.service;

import br.gravita.core.usercases.tax.ConfigureIntegrationCredentialService;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.usercases.system.ConfigureIntegrationCredentialCommand;
import br.gravita.core.ports.outbound.persistence.system.IntegrationCredentialRepositoryPort;
import br.gravita.core.domain.system.IntegrationCredential;
import br.gravita.core.domain.system.IntegrationEnvironment;
import br.gravita.core.domain.system.IntegrationName;
import br.gravita.core.domain.system.UnknownIntegrationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigureIntegrationCredentialServiceTest {

	@Mock
	private IntegrationCredentialRepositoryPort repositoryPort;

	@Test
	@DisplayName("Registers a new credential when none exists yet")
	void shouldRegisterNewCredentialWhenNoneExistsYet() {
		final ConfigureIntegrationCredentialService service = new ConfigureIntegrationCredentialService(repositoryPort);
		when(repositoryPort.findByIntegrationNameAndEnvironment(IntegrationName.BANK, null))
				.thenReturn(Optional.empty());

		service.execute(new ConfigureIntegrationCredentialCommand("bank", null, "https://bank.example.com", "key"));

		final ArgumentCaptor<IntegrationCredential> captor = ArgumentCaptor.forClass(IntegrationCredential.class);
		verify(repositoryPort).save(captor.capture());
		assertThat(captor.getValue().getIntegrationName()).isEqualTo(IntegrationName.BANK);
		assertThat(captor.getValue().getEndpoint()).isEqualTo("https://bank.example.com");
		assertThat(captor.getValue().getCredentialPayload()).isEqualTo("key");
	}

	@Test
	@DisplayName("Rotates the existing credential instead of creating a new one")
	void shouldRotateExistingCredentialInsteadOfCreatingANewOne() {
		final ConfigureIntegrationCredentialService service = new ConfigureIntegrationCredentialService(repositoryPort);
		final IntegrationCredential existing = IntegrationCredential.register(IntegrationName.SEFAZ,
				IntegrationEnvironment.PRODUCTION, "https://old.example.com", "old-cert");
		when(repositoryPort.findByIntegrationNameAndEnvironment(IntegrationName.SEFAZ, IntegrationEnvironment.PRODUCTION))
				.thenReturn(Optional.of(existing));

		service.execute(new ConfigureIntegrationCredentialCommand("sefaz", IntegrationEnvironment.PRODUCTION,
				"https://new.example.com", "new-cert"));

		final ArgumentCaptor<IntegrationCredential> captor = ArgumentCaptor.forClass(IntegrationCredential.class);
		verify(repositoryPort).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(existing.getId());
		assertThat(captor.getValue().getEndpoint()).isEqualTo("https://new.example.com");
		assertThat(captor.getValue().getCredentialPayload()).isEqualTo("new-cert");
	}

	@Test
	@DisplayName("Keeps SEFAZ production and homologation credentials independent")
	void shouldKeepProductionAndHomologationSefazCredentialsIndependent() {
		final ConfigureIntegrationCredentialService service = new ConfigureIntegrationCredentialService(repositoryPort);
		when(repositoryPort.findByIntegrationNameAndEnvironment(IntegrationName.SEFAZ, IntegrationEnvironment.HOMOLOGATION))
				.thenReturn(Optional.empty());

		service.execute(new ConfigureIntegrationCredentialCommand("sefaz", IntegrationEnvironment.HOMOLOGATION,
				"https://homolog.example.com", "homolog-cert"));

		verify(repositoryPort, never()).findByIntegrationNameAndEnvironment(IntegrationName.SEFAZ,
				IntegrationEnvironment.PRODUCTION);
		final ArgumentCaptor<IntegrationCredential> captor = ArgumentCaptor.forClass(IntegrationCredential.class);
		verify(repositoryPort).save(captor.capture());
		assertThat(captor.getValue().getEnvironment()).isEqualTo(IntegrationEnvironment.HOMOLOGATION);
	}

	@Test
	@DisplayName("Rejects an unknown integration name before touching the repository")
	void shouldRejectUnknownIntegrationNameBeforeTouchingTheRepository() {
		final ConfigureIntegrationCredentialService service = new ConfigureIntegrationCredentialService(repositoryPort);

		assertThatThrownBy(() -> service.execute(
				new ConfigureIntegrationCredentialCommand("stripe", null, "https://stripe.example.com", "key")))
				.isInstanceOf(UnknownIntegrationException.class);

		verify(repositoryPort, never()).findByIntegrationNameAndEnvironment(any(), any());
		verify(repositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a SEFAZ credential without an environment")
	void shouldRejectSefazWithoutEnvironment() {
		final ConfigureIntegrationCredentialService service = new ConfigureIntegrationCredentialService(repositoryPort);
		when(repositoryPort.findByIntegrationNameAndEnvironment(IntegrationName.SEFAZ, null)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new ConfigureIntegrationCredentialCommand("sefaz", null, "https://sefaz.example.com", "cert")))
				.isInstanceOf(BusinessRuleException.class);

		verify(repositoryPort, never()).save(any());
	}
}
