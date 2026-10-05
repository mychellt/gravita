package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseMunicipalityUnavailableException;
import br.gravita.core.domain.tax.NfseStandard;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.inbound.tax.CancelNfseCommand;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalityIntegrationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.tax.IssueNfsePort;
import br.gravita.core.ports.outbound.tax.NfseCancellationRequest;
import br.gravita.core.ports.outbound.tax.NfseCancellationResult;
import br.gravita.core.usercases.tax.CancelNfseService;
import java.math.BigDecimal;
import java.time.Instant;
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
class CancelNfseServiceTest {

	private static final String SP = "3550308";
	private static final String JUSTIFICATION = "Servico nao prestado";
	private static final Instant CANCELLED_AT = Instant.parse("2026-10-02T09:00:00Z");

	@Mock
	private NfseRepositoryPort nfseRepositoryPort;
	@Mock
	private MunicipalityIntegrationRepositoryPort integrationRepositoryPort;

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private IssueNfsePort abrasf;
	private IssueNfsePort betha;
	private CancelNfseService service;

	@BeforeEach
	void setUp() {
		abrasf = issuer(NfseStandard.ABRASF);
		betha = issuer(NfseStandard.BETHA);
		service = new CancelNfseService(nfseRepositoryPort, integrationRepositoryPort, List.of(abrasf, betha));
	}

	private static IssueNfsePort issuer(final NfseStandard standard) {
		final IssueNfsePort port = mock(IssueNfsePort.class);
		when(port.standard()).thenReturn(standard);
		return port;
	}

	private NfseDocument rps() {
		return NfseDocument.issueRps()
				.id(NfseId.of(UUID.randomUUID()))
				.providerCompanyId(companyId)
				.providerMunicipalityIbgeCode(SP)
				.tomador(NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null))
				.serviceCode(ServiceCode.of("1.05"))
				.placeOfProvision(PlaceOfProvision.PROVIDER)
				.issMunicipalityIbgeCode(SP)
				.serviceAmount(new BigDecimal("1000.00"))
				.issRate(new BigDecimal("5.0000"))
				.issAmount(new BigDecimal("50.00"))
				.issRateOverrideJustification(null)
				.withholdings(List.of())
				.discrimination("Consultoria")
				.rpsSeries("RPS")
				.rpsNumber(1L)
				.createdAt(Instant.now())
				.build();
	}

	private NfseDocument draft() {
		return rps().convertToNfse("1", 10L, Instant.now());
	}

	private NfseDocument authorized() {
		return draft().send(Instant.now()).authorize("PROT-1", Instant.now(), "xml/ref-1");
	}

	private MunicipalityIntegration integration(final NfseStandard standard, final boolean homologated) {
		return MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()), SP, standard,
				homologated ? "2.04" : null, homologated ? "https://nfse.example/ws" : null, CertificateType.A1,
				List.of("inscricaoMunicipal"), homologated);
	}

	private void stored(final NfseDocument document, final MunicipalityIntegration integration) {
		when(nfseRepositoryPort.findByIdForUpdate(document.getId())).thenReturn(Optional.of(document));
		when(integrationRepositoryPort.findByIbgeCode(SP)).thenReturn(Optional.of(integration));
	}

	private static CancelNfseCommand commandFor(final NfseDocument document) {
		return new CancelNfseCommand(document.getId(), JUSTIFICATION);
	}

	@Test
	@DisplayName("Cancels an authorized document through the adapter of its municipality's standard")
	void ac1and3AnAuthorizedDocumentIsCancelledThroughTheAdapterOfItsMunicipalitysStandard() {
		final NfseDocument authorized = authorized();
		final MunicipalityIntegration integration = integration(NfseStandard.BETHA, true);
		stored(authorized, integration);
		when(betha.cancel(any())).thenReturn(NfseCancellationResult.confirmed(CANCELLED_AT));

		service.execute(commandFor(authorized));

		final ArgumentCaptor<NfseCancellationRequest> request = ArgumentCaptor.forClass(NfseCancellationRequest.class);
		verify(betha).cancel(request.capture());
		assertThat(request.getValue().integration()).isSameAs(integration);
		assertThat(request.getValue().document().getId()).isEqualTo(authorized.getId());
		assertThat(request.getValue().justification()).isEqualTo(JUSTIFICATION);
		verify(abrasf, never()).cancel(any());
	}

	@Test
	@DisplayName("Saves the confirmed cancellation as cancelled with the justification, keeping the same record")
	void ac2and4TheConfirmedCancellationIsSavedAsCancelledWithTheJustificationAndTheSameRecord() {
		final NfseDocument authorized = authorized();
		stored(authorized, integration(NfseStandard.ABRASF, true));
		when(abrasf.cancel(any())).thenReturn(NfseCancellationResult.confirmed(CANCELLED_AT));

		service.execute(commandFor(authorized));

		final ArgumentCaptor<NfseDocument> saved = ArgumentCaptor.forClass(NfseDocument.class);
		verify(nfseRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(authorized.getId());
		assertThat(saved.getValue().getStatus()).isEqualTo(NfseStatus.CANCELLED);
		assertThat(saved.getValue().getCancellationJustification()).isEqualTo(JUSTIFICATION);
		assertThat(saved.getValue().getCancelledAt()).isEqualTo(CANCELLED_AT);
		assertThat(saved.getValue().getProtocol()).isEqualTo("PROT-1");
	}

	@Test
	@DisplayName("Refuses a missing or blank justification before loading or sending anything")
	void ac2AMissingOrBlankJustificationIsRefusedBeforeAnythingIsLoadedOrSent() {
		final NfseId id = NfseId.of(UUID.randomUUID());

		assertThatThrownBy(() -> service.execute(new CancelNfseCommand(id, null)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("justification");
		assertThatThrownBy(() -> service.execute(new CancelNfseCommand(id, "  ")))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("justification");

		verifyNoInteractions(nfseRepositoryPort, integrationRepositoryPort);
		verify(abrasf, never()).cancel(any());
		verify(betha, never()).cancel(any());
	}

	@Test
	@DisplayName("Only an authorized document can be cancelled")
	void ac1OnlyAnAuthorizedDocumentCanBeCancelled() {
		final NfseDocument draft = draft();
		final NfseDocument cancelled = authorized().cancel("motivo", Instant.now());
		final NfseDocument rps = rps();
		when(nfseRepositoryPort.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));
		when(nfseRepositoryPort.findByIdForUpdate(cancelled.getId())).thenReturn(Optional.of(cancelled));
		when(nfseRepositoryPort.findByIdForUpdate(rps.getId())).thenReturn(Optional.of(rps));

		assertThatThrownBy(() -> service.execute(commandFor(draft))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("DRAFT");
		assertThatThrownBy(() -> service.execute(commandFor(cancelled))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CANCELLED");
		assertThatThrownBy(() -> service.execute(commandFor(rps))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("RPS");

		verify(nfseRepositoryPort, never()).save(any());
		verifyNoInteractions(integrationRepositoryPort);
		verify(abrasf, never()).cancel(any());
		verify(betha, never()).cancel(any());
	}

	@Test
	@DisplayName("Reports a municipality refusal with its reason and saves nothing")
	void municipalityRefusalIsReportedWithItsReasonAndNothingIsSaved() {
		final NfseDocument authorized = authorized();
		stored(authorized, integration(NfseStandard.ABRASF, true));
		when(abrasf.cancel(any())).thenReturn(NfseCancellationResult.rejected("E79 - Prazo de cancelamento expirado"));

		assertThatThrownBy(() -> service.execute(commandFor(authorized))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("E79 - Prazo de cancelamento expirado");

		verify(nfseRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Propagates a municipality that does not answer and saves nothing")
	void municipalityThatDoesNotAnswerPropagatesAndNothingIsSaved() {
		final NfseDocument authorized = authorized();
		stored(authorized, integration(NfseStandard.ABRASF, true));
		when(abrasf.cancel(any())).thenThrow(new NfseMunicipalityUnavailableException("timeout", null));

		assertThatThrownBy(() -> service.execute(commandFor(authorized)))
				.isInstanceOf(NfseMunicipalityUnavailableException.class);

		verify(nfseRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Treats a standard without an adapter or a non-homologated municipality as a business rule violation")
	void standardWithoutAnAdapterOrANonHomologatedMunicipalityIsABusinessRuleViolation() {
		final NfseDocument authorized = authorized();
		stored(authorized, integration(NfseStandard.NFSE_NACIONAL, true));
		assertThatThrownBy(() -> service.execute(commandFor(authorized))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("NFSE_NACIONAL");

		when(integrationRepositoryPort.findByIbgeCode(SP))
				.thenReturn(Optional.of(integration(NfseStandard.ABRASF, false)));
		assertThatThrownBy(() -> service.execute(commandFor(authorized))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("homologated");

		when(integrationRepositoryPort.findByIbgeCode(SP)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.execute(commandFor(authorized))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining(SP);

		verify(nfseRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects two adapters registered for the same standard at wiring time")
	void twoAdaptersForTheSameStandardAreRejectedAtWiring() {
		assertThatThrownBy(() -> new CancelNfseService(nfseRepositoryPort, integrationRepositoryPort,
				List.of(abrasf, issuer(NfseStandard.ABRASF)))).isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("Reports not found for an unknown document")
	void anUnknownDocumentIsNotFound() {
		final NfseId id = NfseId.of(UUID.randomUUID());
		when(nfseRepositoryPort.findByIdForUpdate(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new CancelNfseCommand(id, JUSTIFICATION)))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
