package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.MunicipalityIntegrationJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.MunicipalityIntegrationPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.MunicipalityIntegrationJpaRepository;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import br.gravita.core.domain.tax.NfseStandard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MunicipalityIntegrationRepositoryAdapterTest {

	@Mock
	private MunicipalityIntegrationJpaRepository repository;

	@Mock
	private MunicipalityIntegrationPersistenceMapper mapper;

	@InjectMocks
	private MunicipalityIntegrationRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new municipality integration using the mapped entity")
	void savesANewMunicipalityIntegrationUsingTheMappedEntity() {
		final MunicipalityIntegration integration = buildIntegration();
		final MunicipalityIntegrationJpaEntity entity = buildEntity(integration.getId().value());
		final MunicipalityIntegrationJpaEntity saved = buildEntity(integration.getId().value());
		when(repository.findById(integration.getId().value())).thenReturn(Optional.empty());
		when(mapper.map(integration)).thenReturn(entity);
		when(repository.save(same(entity))).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(integration);

		final MunicipalityIntegration result = adapter.save(integration);

		assertThat(result).isSameAs(integration);
		verify(repository).save(same(entity));
	}

	@Test
	@DisplayName("Updates the managed row in place when the integration already exists")
	void savingAnUpdatedIntegrationUpdatesTheManagedRow() {
		final MunicipalityIntegration integration = buildIntegration();
		final MunicipalityIntegrationJpaEntity existing = buildEntity(integration.getId().value());
		final MunicipalityIntegrationJpaEntity fresh = buildEntity(integration.getId().value());
		fresh.setStandard(NfseStandard.BETHA);
		fresh.setVersion("3.00");
		fresh.setWebserviceUrl("https://new");
		fresh.setRequiredCertificateType(CertificateType.A3);
		fresh.setRequiredFields(List.of("c"));
		fresh.setHomologated(false);
		when(repository.findById(integration.getId().value())).thenReturn(Optional.of(existing));
		when(mapper.map(integration)).thenReturn(fresh);
		when(repository.save(same(existing))).thenReturn(existing);
		when(mapper.map(existing)).thenReturn(integration);

		adapter.save(integration);

		assertThat(existing.getStandard()).isEqualTo(NfseStandard.BETHA);
		assertThat(existing.getVersion()).isEqualTo("3.00");
		assertThat(existing.getWebserviceUrl()).isEqualTo("https://new");
		assertThat(existing.getRequiredCertificateType()).isEqualTo(CertificateType.A3);
		assertThat(existing.getRequiredFields()).containsExactly("c");
		assertThat(existing.isHomologated()).isFalse();
		verify(repository).save(same(existing));
	}

	@Test
	@DisplayName("Finds a municipality integration by IBGE code")
	void findsByIbgeCode() {
		final MunicipalityIntegration integration = buildIntegration();
		final MunicipalityIntegrationJpaEntity entity = buildEntity(integration.getId().value());
		when(repository.findByIbgeCode("3550308")).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(integration);

		final Optional<MunicipalityIntegration> result = adapter.findByIbgeCode("3550308");

		assertThat(result).contains(integration);
		verify(repository).findByIbgeCode("3550308");
	}

	@Test
	@DisplayName("Returns empty when no integration exists for the IBGE code")
	void returnsEmptyWhenNoIntegrationExistsForTheIbgeCode() {
		when(repository.findByIbgeCode("4106902")).thenReturn(Optional.empty());

		assertThat(adapter.findByIbgeCode("4106902")).isEmpty();
	}

	private MunicipalityIntegrationJpaEntity buildEntity(final UUID id) {
		return MunicipalityIntegrationJpaEntity.builder().id(id).ibgeCode("3550308").build();
	}

	private MunicipalityIntegration buildIntegration() {
		return MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()), "3550308",
				NfseStandard.ABRASF, "2.04", "https://nfse.example/ws", CertificateType.A1,
				List.of("inscricaoMunicipal", "codigoTributacao"), true);
	}
}
