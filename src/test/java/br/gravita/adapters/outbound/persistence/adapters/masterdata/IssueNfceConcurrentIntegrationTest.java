package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.entities.masterdata.CompanyJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.DocumentSeriesPersistenceMapperImpl;
import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.Payment;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.SaleItem;
import br.gravita.core.domain.tax.SefazUnavailableException;
import br.gravita.core.ports.inbound.tax.IssueNfceCommand;
import br.gravita.core.ports.inbound.tax.NfceIssuanceResult;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.usercases.AllocateDocumentNumberService;
import br.gravita.core.usercases.tax.IssueNfceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({DocumentSeriesRepositoryAdapter.class, DocumentSeriesPersistenceMapperImpl.class})
class IssueNfceConcurrentIntegrationTest {

	@Autowired
	private DocumentSeriesRepositoryAdapter repositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("Never allocates the same document number to two racing NFC-e issuances")
	void twoRacingIssuancesNeverAllocateTheSameDocumentNumber() {
		CompanyId companyId = persistCompany();
		repositoryAdapter.save(DocumentSeries.placeholder(companyId, FiscalDocumentType.NFCE).reconfigure("001", 700L));
		entityManager.flush();
		entityManager.clear();

		DocumentSeries staleSnapshot = repositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFCE)
				.orElseThrow();
		DocumentSeriesRepositoryPort racingPort = racingPortFor(staleSnapshot);

		IssueNfceService winnerService = issueServiceFor(companyId, repositoryAdapter);
		IssueNfceService loserService = issueServiceFor(companyId, racingPort);

		NfceIssuanceResult winnerResult = winnerService.execute(new IssueNfceCommand(UUID.randomUUID()));
		entityManager.flush();
		entityManager.clear();

		NfceIssuanceResult loserResult = loserService.execute(new IssueNfceCommand(UUID.randomUUID()));

		String winnerNumber = winnerResult.accessKey().substring(25, 34);
		String loserNumber = loserResult.accessKey().substring(25, 34);
		assertThat(winnerNumber).isEqualTo("000000700");
		assertThat(loserNumber).isEqualTo("000000701");
		assertThat(loserNumber).isNotEqualTo(winnerNumber);
	}

	private IssueNfceService issueServiceFor(CompanyId companyId, DocumentSeriesRepositoryPort documentSeriesPort) {
		PosSessionId sessionId = PosSessionId.of(UUID.randomUUID());
		return new IssueNfceService(fakeNfceRepositoryPort(sessionId), fakePosSessionRepositoryPort(sessionId, companyId),
				fakeCompanyRepositoryPort(companyId),
				command -> new br.gravita.core.ports.inbound.tax.TaxCalculationResult(List.of(),
						br.gravita.core.domain.tax.TaxCalculationTotals.from(List.of())),
				new AllocateDocumentNumberService(documentSeriesPort),
				new br.gravita.core.ports.outbound.tax.SubmitToSefazPort() {
					@Override
					public br.gravita.core.ports.outbound.tax.SefazSubmissionResult submit(
							br.gravita.core.ports.outbound.tax.SefazSubmissionRequest request) {
						throw new SefazUnavailableException("no SEFAZ in this test", null);
					}

					@Override
					public br.gravita.core.ports.outbound.tax.SefazSubmissionResult cancel(
							br.gravita.core.ports.outbound.tax.SefazCancellationRequest request) {
						throw new UnsupportedOperationException("not exercised by this test");
					}

					@Override
					public br.gravita.core.ports.outbound.tax.SefazSubmissionResult voidNumberRange(
							br.gravita.core.ports.outbound.tax.SefazVoidNumberRangeRequest request) {
						throw new UnsupportedOperationException("not exercised by this test");
					}

					@Override
					public br.gravita.core.ports.outbound.tax.SefazSubmissionResult correct(
							br.gravita.core.ports.outbound.tax.SefazCorrectionRequest request) {
						throw new UnsupportedOperationException("not exercised by this test");
					}

					@Override
					public br.gravita.core.ports.outbound.tax.SefazSubmissionResult manifest(
							br.gravita.core.ports.outbound.tax.SefazManifestationRequest request) {
						throw new UnsupportedOperationException("not exercised by this test");
					}
				}, new br.gravita.core.ports.outbound.tax.TransmissionQueuePort() {
					@Override
					public void enqueue(br.gravita.core.domain.tax.TransmissionQueueId id) {
					}

					@Override
					public List<br.gravita.core.domain.tax.TransmissionQueueEntry> findDue(Instant asOf) {
						throw new UnsupportedOperationException("not exercised by this test");
					}

					@Override
					public void reschedule(UUID documentId, int attempts, Instant nextRetryAt) {
						throw new UnsupportedOperationException("not exercised by this test");
					}

					@Override
					public void remove(UUID documentId) {
						throw new UnsupportedOperationException("not exercised by this test");
					}
				});
	}

	private NfceRepositoryPort fakeNfceRepositoryPort(PosSessionId sessionId) {
		return new NfceRepositoryPort() {
			@Override
			public NfceSale save(NfceSale sale) {
				return sale;
			}

			@Override
			public Optional<NfceSale> findById(NfceSaleId id) {
				SaleItem item = new SaleItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("10.00"), null);
				return Optional.of(NfceSale.register(id, sessionId, List.of(item), null,
						List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))), null, Instant.now()));
			}

			@Override
			public Optional<NfceSale> findMostRecent() {
				throw new UnsupportedOperationException("not exercised by this test");
			}

			@Override
			public List<NfceSale> findBySessionId(PosSessionId id) {
				throw new UnsupportedOperationException("not exercised by this test");
			}

			@Override
			public List<NfceSale> findAuthorizedBetween(Instant from, Instant to) {
				throw new UnsupportedOperationException("not exercised by this test");
			}
		};
	}

	private PosSessionRepositoryPort fakePosSessionRepositoryPort(PosSessionId sessionId, CompanyId companyId) {
		PosSession session = PosSession.open(sessionId, UUID.randomUUID(), UUID.randomUUID(), companyId,
				BigDecimal.ZERO, Instant.now());
		return new PosSessionRepositoryPort() {
			@Override
			public PosSession save(PosSession posSession) {
				return posSession;
			}

			@Override
			public Optional<PosSession> findById(PosSessionId id) {
				return Optional.of(session);
			}

			@Override
			public boolean existsByRegisterIdAndStatus(UUID registerId,
					br.gravita.core.domain.tax.PosSessionStatus status) {
				return false;
			}
		};
	}

	private br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort fakeCompanyRepositoryPort(CompanyId companyId) {
		Company company = Company.of(companyId, "Acme Ltda", br.gravita.core.domain.shared.Document.cnpj("11222333000181"),
				"123456789", "987654", "6201500", TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION,
				"Rua Teste, 100", "SP", "nfce@example.com", "11999999999", null, null);
		return new br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort() {
			@Override
			public Company save(Company c) {
				return c;
			}

			@Override
			public Optional<Company> findById(CompanyId id) {
				return Optional.of(company);
			}
		};
	}

	private DocumentSeriesRepositoryPort racingPortFor(DocumentSeries staleSnapshot) {
		return new DocumentSeriesRepositoryPort() {
			private boolean firstRead = true;

			@Override
			public DocumentSeries save(DocumentSeries documentSeries) {
				return repositoryAdapter.save(documentSeries);
			}

			@Override
			public Optional<DocumentSeries> findByCompanyIdAndDocumentType(CompanyId companyId,
					FiscalDocumentType documentType) {
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
				.cnpj(UUID.randomUUID().toString().substring(0, 14))
				.ie("123456789")
				.im("987654")
				.cnae("6201500")
				.taxRegime(br.gravita.core.domain.masterdata.TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(true)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Teste, 100")
				.issuingEmail("nfe@example.com")
				.phone("11999999999")
				.build());
		return CompanyId.of(id);
	}
}
