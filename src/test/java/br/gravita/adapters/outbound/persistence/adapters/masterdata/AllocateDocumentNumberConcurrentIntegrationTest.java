package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.CompanyJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.DocumentSeriesPersistenceMapperImpl;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.usercases.AllocateDocumentNumberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({DocumentSeriesRepositoryAdapter.class, DocumentSeriesPersistenceMapperImpl.class})
class AllocateDocumentNumberConcurrentIntegrationTest {

	@Autowired
	private DocumentSeriesRepositoryAdapter repositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("Retries against a fresh read when a stale write loses the race, instead of failing")
	void aStaleWriteLosingTheRaceRetriesAgainstAFreshReadInsteadOfFailing() {
		CompanyId companyId = persistCompany();
		repositoryAdapter.save(DocumentSeries.placeholder(companyId, FiscalDocumentType.NFE).reconfigure("001", 500L));
		entityManager.flush();
		entityManager.clear();

		DocumentSeries staleSnapshot = repositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE)
				.orElseThrow();
		DocumentSeriesRepositoryPort racingPort = racingPortFor(staleSnapshot);
		AllocateDocumentNumberService loserService = new AllocateDocumentNumberService(racingPort);

		AllocateDocumentNumberService winnerService = new AllocateDocumentNumberService(repositoryAdapter);
		DocumentNumber winnerNumber = winnerService.execute(new AllocateDocumentNumberCommand(companyId, FiscalDocumentType.NFE));
		entityManager.flush();
		entityManager.clear();
		assertThat(winnerNumber.number()).isEqualTo(500L);

		DocumentNumber loserNumber = loserService.execute(new AllocateDocumentNumberCommand(companyId, FiscalDocumentType.NFE));

		assertThat(loserNumber.number()).isEqualTo(501L);
		assertThat(loserNumber.number()).isNotEqualTo(winnerNumber.number());

		DocumentSeries persisted = repositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE)
				.orElseThrow();
		assertThat(persisted.getNextNumber()).isEqualTo(502L);
	}

	private DocumentSeriesRepositoryPort racingPortFor(DocumentSeries staleSnapshot) {
		return new DocumentSeriesRepositoryPort() {
			private boolean firstRead = true;

			@Override
			public DocumentSeries save(DocumentSeries documentSeries) {
				return repositoryAdapter.save(documentSeries);
			}

			@Override
			public Optional<DocumentSeries> findByCompanyIdAndDocumentType(CompanyId companyId, FiscalDocumentType documentType) {
				if (firstRead) {
					firstRead = false;
					return Optional.of(staleSnapshot);
				}
				return repositoryAdapter.findByCompanyIdAndDocumentType(companyId, documentType);
			}
		};
	}

	private CompanyId persistCompany() {
		UUID id = UUID.randomUUID();
		entityManager.persist(CompanyJpaEntity.builder()
				.id(id)
				.name("Acme Ltda")
				.document(UUID.randomUUID().toString().substring(0, 14))
				.ie("123456789")
				.im("987654")
				.cnae("6201500")
				.taxRegime(TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(true)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Teste, 100")
				.issuingEmail("nfe@example.com")
				.phone("11999999999")
				.build());
		return CompanyId.of(id);
	}
}
