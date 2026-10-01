package br.gravita.adapters.outbound.persistence.adapters.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.mappers.tax.MunicipalityIntegrationPersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.repositories.tax.MunicipalityIntegrationJpaRepository;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import br.gravita.core.domain.tax.NfseStandard;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({MunicipalityIntegrationRepositoryAdapter.class, MunicipalityIntegrationPersistenceMapperImpl.class})
class MunicipalityIntegrationRepositoryAdapterTest {

	@Autowired
	private MunicipalityIntegrationRepositoryAdapter repositoryAdapter;

	@Autowired
	private MunicipalityIntegrationJpaRepository jpaRepository;

	@Test
	void savesAndFindsByIbgeCodeWithRequiredFields() {
		repositoryAdapter.save(MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()), "3550308",
				NfseStandard.ABRASF, "2.04", "https://nfse.example/ws", CertificateType.A1,
				List.of("inscricaoMunicipal", "codigoTributacao"), true));

		MunicipalityIntegration found = repositoryAdapter.findByIbgeCode("3550308").orElseThrow();

		assertThat(found.getStandard()).isEqualTo(NfseStandard.ABRASF);
		assertThat(found.getRequiredFields()).containsExactly("inscricaoMunicipal", "codigoTributacao");
		assertThat(found.isHomologated()).isTrue();
		assertThat(repositoryAdapter.findByIbgeCode("4106902")).isEmpty();
	}

	@Test
	void ac3_savingAnUpdatedIntegrationKeepsASingleRowPerIbgeCode() {
		MunicipalityIntegration integration = repositoryAdapter
				.save(MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()), "3550308",
						NfseStandard.ABRASF, "2.04", "https://old", CertificateType.A1, List.of("a", "b"), true));

		MunicipalityIntegration reloaded = repositoryAdapter.findByIbgeCode("3550308").orElseThrow();
		reloaded.update(NfseStandard.BETHA, null, null, CertificateType.A3, List.of("c"), false);
		repositoryAdapter.save(reloaded);
		jpaRepository.flush();

		assertThat(jpaRepository.count()).isEqualTo(1);
		MunicipalityIntegration updated = repositoryAdapter.findByIbgeCode("3550308").orElseThrow();
		assertThat(updated.getId()).isEqualTo(integration.getId());
		assertThat(updated.getStandard()).isEqualTo(NfseStandard.BETHA);
		assertThat(updated.getRequiredFields()).containsExactly("c");
		assertThat(updated.isHomologated()).isFalse();
	}
}
