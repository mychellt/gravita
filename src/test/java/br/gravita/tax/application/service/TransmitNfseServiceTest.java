package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
import br.gravita.core.ports.inbound.tax.NfseTransmissionResult;
import br.gravita.core.ports.inbound.tax.TransmitNfseCommand;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalityIntegrationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.tax.GenerateGuidedManualUploadPort;
import br.gravita.core.ports.outbound.tax.IssueNfsePort;
import br.gravita.core.ports.outbound.tax.NfseIssueRequest;
import br.gravita.core.ports.outbound.tax.NfseIssueResult;
import br.gravita.core.usercases.tax.TransmitNfseService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransmitNfseServiceTest {

	private static final String SP = "3550308";
	private static final Instant AUTHORIZED_AT = Instant.parse("2026-10-01T12:00:00Z");
	private static final byte[] XML = "<xml/>".getBytes(StandardCharsets.UTF_8);

	@Mock
	private NfseRepositoryPort nfseRepositoryPort;
	@Mock
	private MunicipalityIntegrationRepositoryPort integrationRepositoryPort;
	@Mock
	private GenerateGuidedManualUploadPort guidedManualUploadPort;
	@Mock
	private XmlObjectStoragePort xmlObjectStoragePort;

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private IssueNfsePort abrasf;
	private IssueNfsePort betha;
	private TransmitNfseService service;

	@BeforeEach
	void setUp() {
		abrasf = issuer(NfseStandard.ABRASF);
		betha = issuer(NfseStandard.BETHA);
		service = new TransmitNfseService(nfseRepositoryPort, integrationRepositoryPort, guidedManualUploadPort,
				xmlObjectStoragePort, List.of(abrasf, betha));
	}

	private static IssueNfsePort issuer(final NfseStandard standard) {
		final IssueNfsePort port = mock(IssueNfsePort.class);
		when(port.standard()).thenReturn(standard);
		return port;
	}

	private NfseDocument draft() {
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
				.build().convertToNfse("1", 10L, Instant.now());
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

	private static TransmitNfseCommand commandFor(final NfseDocument document) {
		return new TransmitNfseCommand(document.getId());
	}

	@Test
	@DisplayName("Transmits a homologated document through the adapter of its municipality's standard")
	void ac1AHomologatedDocumentIsTransmittedThroughTheAdapterOfItsMunicipalitysStandard() {
		final NfseDocument draft = draft();
		final MunicipalityIntegration integration = integration(NfseStandard.BETHA, true);
		stored(draft, integration);
		when(betha.issue(any())).thenReturn(NfseIssueResult.authorized("PROT-1", AUTHORIZED_AT, XML));
		when(xmlObjectStoragePort.store(companyId, XML)).thenReturn("ref-1");

		final NfseTransmissionResult result = service.execute(commandFor(draft));

		assertThat(result).isEqualTo(new NfseTransmissionResult.Authorized("PROT-1", AUTHORIZED_AT));
		final ArgumentCaptor<NfseIssueRequest> request = ArgumentCaptor.forClass(NfseIssueRequest.class);
		verify(betha).issue(request.capture());
		assertThat(request.getValue().integration()).isSameAs(integration);
		assertThat(request.getValue().document().getId()).isEqualTo(draft.getId());
		verify(abrasf, never()).issue(any());
		verifyNoInteractions(guidedManualUploadPort);
	}

	@Test
	@DisplayName("Marks the document sent, then authorized, with the protocol and only the XML reference")
	void ac3and5SuccessMarksTheDocumentSentThenAuthorizedWithProtocolAndOnlyTheXmlReference() {
		final NfseDocument draft = draft();
		stored(draft, integration(NfseStandard.ABRASF, true));
		when(abrasf.issue(any())).thenReturn(NfseIssueResult.authorized("PROT-1", AUTHORIZED_AT, XML));
		when(xmlObjectStoragePort.store(companyId, XML)).thenReturn("ref-1");

		service.execute(commandFor(draft));

		final ArgumentCaptor<NfseDocument> saved = ArgumentCaptor.forClass(NfseDocument.class);
		final InOrder order = inOrder(nfseRepositoryPort, abrasf, xmlObjectStoragePort);
		order.verify(nfseRepositoryPort).save(saved.capture());
		order.verify(abrasf).issue(any());
		order.verify(xmlObjectStoragePort).store(companyId, XML);
		order.verify(nfseRepositoryPort).save(saved.capture());
		assertThat(saved.getAllValues()).extracting(NfseDocument::getStatus)
				.containsExactly(NfseStatus.SENT, NfseStatus.AUTHORIZED);
		final NfseDocument authorized = saved.getAllValues().get(1);
		assertThat(authorized.getProtocol()).isEqualTo("PROT-1");
		assertThat(authorized.getAuthorizedAt()).isEqualTo(AUTHORIZED_AT);
		assertThat(authorized.getXmlReference()).isEqualTo("ref-1");
	}

	@Test
	@DisplayName("Treats a rejection as a result and leaves the document a draft that can be transmitted again")
	void ac3ARejectionIsAResultAndLeavesTheDocumentADraftThatCanBeTransmittedAgain() {
		final NfseDocument draft = draft();
		stored(draft, integration(NfseStandard.ABRASF, true));
		when(abrasf.issue(any())).thenReturn(NfseIssueResult.rejected("E160 - Arquivo em desacordo"));

		final NfseTransmissionResult result = service.execute(commandFor(draft));

		assertThat(result).isEqualTo(new NfseTransmissionResult.Rejected("E160 - Arquivo em desacordo"));
		final ArgumentCaptor<NfseDocument> saved = ArgumentCaptor.forClass(NfseDocument.class);
		verify(nfseRepositoryPort, org.mockito.Mockito.times(2)).save(saved.capture());
		final NfseDocument last = saved.getAllValues().get(1);
		assertThat(last.getStatus()).isEqualTo(NfseStatus.DRAFT);
		assertThat(last.getLastRejectionReason()).isEqualTo("E160 - Arquivo em desacordo");
		assertThat(last.send(Instant.now()).getStatus()).isEqualTo(NfseStatus.SENT);
		verifyNoInteractions(xmlObjectStoragePort);
	}

	@Test
	@DisplayName("Propagates a municipality that does not answer and never saves an authorized document")
	void ac3AMunicipalityThatDoesNotAnswerPropagatesAndNeverSavesAnAuthorizedDocument() {
		final NfseDocument draft = draft();
		stored(draft, integration(NfseStandard.ABRASF, true));
		when(abrasf.issue(any())).thenThrow(new NfseMunicipalityUnavailableException("timeout", null));

		assertThatThrownBy(() -> service.execute(commandFor(draft)))
				.isInstanceOf(NfseMunicipalityUnavailableException.class);

		// Only the in-flight SENT save happened; the transaction around execute() rolls it back (see the e2e test).
		final ArgumentCaptor<NfseDocument> saved = ArgumentCaptor.forClass(NfseDocument.class);
		verify(nfseRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(NfseStatus.SENT);
		verifyNoInteractions(xmlObjectStoragePort);
	}

	@Test
	@DisplayName("Gives a non-homologated municipality the guided upload instead of a transmission")
	void ac2ANonHomologatedMunicipalityGetsTheGuidedUploadInsteadOfATransmission() {
		final NfseDocument draft = draft();
		final MunicipalityIntegration integration = integration(NfseStandard.ISSNET, false);
		stored(draft, integration);
		final NfseTransmissionResult.GuidedManualUpload guided = new NfseTransmissionResult.GuidedManualUpload("<xml/>",
				"Upload it");
		when(guidedManualUploadPort.generate(draft, integration)).thenReturn(guided);

		final NfseTransmissionResult result = service.execute(commandFor(draft));

		assertThat(result).isSameAs(guided);
		verify(nfseRepositoryPort, never()).save(any());
		verify(abrasf, never()).issue(any());
		verify(betha, never()).issue(any());
		verifyNoInteractions(xmlObjectStoragePort);
	}

	@Test
	@DisplayName("Fails before the document leaves draft when its standard has no adapter")
	void ac4AStandardWithoutAnAdapterFailsBeforeTheDocumentLeavesDraft() {
		final NfseDocument draft = draft();
		stored(draft, integration(NfseStandard.NFSE_NACIONAL, true));

		assertThatThrownBy(() -> service.execute(commandFor(draft))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("NFSE_NACIONAL");

		verify(nfseRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Needs nothing but registering an adapter to support a new standard")
	void ac4RegisteringAnAdapterForANewStandardIsAllItTakes() {
		final NfseDocument draft = draft();
		stored(draft, integration(NfseStandard.NFSE_NACIONAL, true));
		final IssueNfsePort nacional = issuer(NfseStandard.NFSE_NACIONAL);
		when(nacional.issue(any())).thenReturn(NfseIssueResult.authorized("N-1", AUTHORIZED_AT, XML));
		when(xmlObjectStoragePort.store(companyId, XML)).thenReturn("ref");
		final TransmitNfseService extended = new TransmitNfseService(nfseRepositoryPort, integrationRepositoryPort,
				guidedManualUploadPort, xmlObjectStoragePort, List.of(abrasf, betha, nacional));

		assertThat(extended.execute(commandFor(draft))).isEqualTo(
				new NfseTransmissionResult.Authorized("N-1", AUTHORIZED_AT));
	}

	@Test
	@DisplayName("Rejects two adapters registered for the same standard at wiring time")
	void twoAdaptersForTheSameStandardAreRejectedAtWiring() {
		assertThatThrownBy(() -> new TransmitNfseService(nfseRepositoryPort, integrationRepositoryPort,
				guidedManualUploadPort, xmlObjectStoragePort, List.of(abrasf, issuer(NfseStandard.ABRASF))))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("Reports not found for an unknown document")
	void anUnknownDocumentIsNotFound() {
		final NfseId id = NfseId.of(UUID.randomUUID());
		when(nfseRepositoryPort.findByIdForUpdate(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new TransmitNfseCommand(id)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("Only a draft is transmittable")
	void onlyADraftIsTransmittable() {
		final NfseDocument draft = draft();
		final NfseDocument authorized = draft.send(Instant.now()).authorize("P", Instant.now(), "ref");
		final NfseDocument rps = NfseDocument.issueRps()
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
				.rpsNumber(2L)
				.createdAt(Instant.now())
				.build();
		when(nfseRepositoryPort.findByIdForUpdate(authorized.getId())).thenReturn(Optional.of(authorized));
		when(nfseRepositoryPort.findByIdForUpdate(rps.getId())).thenReturn(Optional.of(rps));

		assertThatThrownBy(() -> service.execute(commandFor(authorized))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("AUTHORIZED");
		assertThatThrownBy(() -> service.execute(commandFor(rps))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("RPS");

		verify(nfseRepositoryPort, never()).save(any());
		verifyNoInteractions(integrationRepositoryPort);
	}

	@Test
	@DisplayName("Treats a municipality without a registered integration as a business rule violation")
	void municipalityWithoutARegisteredIntegrationIsABusinessRuleViolation() {
		final NfseDocument draft = draft();
		when(nfseRepositoryPort.findByIdForUpdate(draft.getId())).thenReturn(Optional.of(draft));
		when(integrationRepositoryPort.findByIbgeCode(SP)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(commandFor(draft))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining(SP);
	}
}
