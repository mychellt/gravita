package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.RpsId;
import br.gravita.core.domain.tax.ServiceTaxRule;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.WithholdingMode;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberUseCase;
import br.gravita.core.ports.inbound.tax.IssueRpsCommand;
import br.gravita.core.ports.inbound.tax.IssueRpsCommand.AddressCommand;
import br.gravita.core.ports.inbound.tax.IssueRpsCommand.TomadorCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalServiceCodeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.ServiceTaxRuleRepositoryPort;
import br.gravita.core.usercases.tax.IssueRpsService;
import java.math.BigDecimal;
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
class IssueRpsServiceTest {

	private static final String SP = "3550308";
	private static final String RIO = "3304557";
	private static final String CNPJ = "11222333000181";
	private static final String CPF = "52998224725";

	@Mock
	private NfseRepositoryPort nfseRepositoryPort;
	@Mock
	private CompanyRepositoryPort companyRepositoryPort;
	@Mock
	private ServiceTaxRuleRepositoryPort serviceTaxRuleRepositoryPort;
	@Mock
	private MunicipalServiceCodeRepositoryPort municipalServiceCodeRepositoryPort;
	@Mock
	private AllocateDocumentNumberUseCase allocateDocumentNumberUseCase;

	private IssueRpsService service;
	private CompanyId companyId;

	@BeforeEach
	void setUp() {
		service = new IssueRpsService(nfseRepositoryPort, companyRepositoryPort, serviceTaxRuleRepositoryPort,
				municipalServiceCodeRepositoryPort, allocateDocumentNumberUseCase);
		companyId = CompanyId.of(UUID.randomUUID());
		org.mockito.Mockito.lenient().when(companyRepositoryPort.findById(companyId))
				.thenReturn(Optional.of(company(br.gravita.core.domain.masterdata.TaxRegime.LUCRO_PRESUMIDO)));
		org.mockito.Mockito.lenient().when(allocateDocumentNumberUseCase.execute(any()))
				.thenReturn(new DocumentNumber("RPS1", 42L));
		org.mockito.Mockito.lenient().when(nfseRepositoryPort.save(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	private Company company(final br.gravita.core.domain.masterdata.TaxRegime regime) {
		return Company.builder()
				.id(companyId)
				.name("Acme Ltda")
				.cnpj(Document.cnpj(CNPJ))
				.ie("123456789")
				.im("987654")
				.cnae("6201500")
				.taxRegime(regime)
				.simplesOptante(false)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Teste, 100")
				.state("SP")
				.issuingEmail("nfse@example.com")
				.phone("11999999999")
				.logoUrl(null)
				.parentCompanyId(null)
				.build();
	}

	private static ServiceTaxRule rule(final String municipality, final TaxRegime regime, final TaxType type, final String rate,
			final WithholdingMode mode) {
		return new ServiceTaxRule("01.05", municipality, regime, type, new BigDecimal(rate), mode);
	}

	private static List<ServiceTaxRule> standardRules() {
		return List.of(
				rule(null, null, TaxType.ISS, "2.0000", WithholdingMode.NEVER),
				rule(SP, null, TaxType.ISS, "5.0000", WithholdingMode.TOMADOR_COMPANY),
				rule(null, null, TaxType.PIS, "0.6500", WithholdingMode.TOMADOR_COMPANY),
				rule(null, null, TaxType.COFINS, "3.0000", WithholdingMode.TOMADOR_COMPANY),
				rule(null, null, TaxType.CSLL, "1.0000", WithholdingMode.TOMADOR_COMPANY),
				rule(null, null, TaxType.IRPJ, "1.5000", WithholdingMode.TOMADOR_COMPANY),
				rule(null, null, TaxType.INSS, "11.0000", WithholdingMode.NEVER));
	}

	private static TomadorCommand pjTomador(final String municipality, final boolean withAddress) {
		return new TomadorCommand(null, CNPJ, PersonType.COMPANY, "Tomador SA", municipality,
				withAddress ? new AddressCommand("Rua A", "10", null, "Centro", "01001000", "SP") : null);
	}

	private static TomadorCommand pfTomador() {
		return new TomadorCommand(null, CPF, PersonType.INDIVIDUAL, "Pessoa Fisica", null, null);
	}

	private IssueRpsCommand command(final TomadorCommand tomador, final PlaceOfProvision place, final String serviceCode,
			final BigDecimal override, final String justification) {
		return new IssueRpsCommand(companyId.value(), SP, tomador, serviceCode, place, new BigDecimal("1000.00"),
				"Desenvolvimento de software", override, justification);
	}

	private IssueRpsCommand command(final TomadorCommand tomador) {
		return command(tomador, PlaceOfProvision.PROVIDER, "1.05", null, null);
	}

	private NfseDocument savedDocument() {
		final ArgumentCaptor<NfseDocument> captor = ArgumentCaptor.forClass(NfseDocument.class);
		verify(nfseRepositoryPort).save(captor.capture());
		return captor.getValue();
	}

	@Test
	@DisplayName("Defaults to the most specific configured ISS rate and computes the withholdings from the rules")
	void ac3and4DefaultsToTheMostSpecificConfiguredIssRateAndComputesTheWithholdingsFromTheRules() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(standardRules());

		final RpsId id = service.execute(command(pjTomador(SP, true)));

		final NfseDocument saved = savedDocument();
		assertThat(id.value()).isEqualTo(saved.getId().value());
		assertThat(saved.getStatus()).isEqualTo(NfseStatus.RPS);
		assertThat(saved.getIssRate()).isEqualByComparingTo("5.0000");
		assertThat(saved.getIssAmount()).isEqualByComparingTo("50.00");
		assertThat(saved.isIssRateOverridden()).isFalse();
		// PJ tomador: ISS + PIS + COFINS + CSLL + IRPJ are withheld; INSS is NEVER for this rule table.
		assertThat(saved.getWithholdings()).extracting("taxType")
				.containsExactlyInAnyOrder(TaxType.ISS, TaxType.PIS, TaxType.COFINS, TaxType.CSLL, TaxType.IRPJ);
		assertThat(saved.getWithholdings()).filteredOn(w -> w.taxType() == TaxType.PIS)
				.singleElement().satisfies(w -> {
					assertThat(w.amount()).isEqualByComparingTo("6.50");
					assertThat(w.base()).isEqualByComparingTo("1000.00");
				});
		assertThat(saved.getWithholdings()).filteredOn(w -> w.taxType() == TaxType.IRPJ)
				.singleElement().satisfies(w -> assertThat(w.amount()).isEqualByComparingTo("15.00"));
	}

	@Test
	@DisplayName("Withholds nothing for an individual tomador and needs no address, as it is not a withholding agent")
	void ac4APfTomadorIsNotAWithholdingAgentSoNothingIsWithheldAndNoAddressIsNeeded() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(standardRules());

		service.execute(command(pfTomador()));

		final NfseDocument saved = savedDocument();
		assertThat(saved.getWithholdings()).isEmpty();
		assertThat(saved.getIssRate()).isEqualByComparingTo("5.0000");
	}

	@Test
	@DisplayName("Follows the rule table, not the code, when determining withholdings")
	void ac4WithholdingsFollowTheRuleTableNotTheCode() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(List.of(
				rule(SP, null, TaxType.ISS, "3.0000", WithholdingMode.NEVER),
				rule(null, null, TaxType.INSS, "11.0000", WithholdingMode.ALWAYS)));

		service.execute(command(pjTomador(SP, true)));

		final NfseDocument saved = savedDocument();
		assertThat(saved.getWithholdings()).extracting("taxType").containsExactly(TaxType.INSS);
		assertThat(saved.getWithholdings().get(0).amount()).isEqualByComparingTo("110.00");
	}

	@Test
	@DisplayName("Ranks a regime-specific rule above a wildcard one")
	void ac4ARegimeSpecificRuleOutranksAWildcardOne() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(List.of(
				rule(SP, null, TaxType.ISS, "5.0000", WithholdingMode.NEVER),
				rule(SP, TaxRegime.LUCRO_PRESUMIDO, TaxType.ISS, "3.5000", WithholdingMode.NEVER),
				rule(SP, TaxRegime.SIMPLES_NACIONAL, TaxType.ISS, "2.0000", WithholdingMode.NEVER),
				rule(null, TaxRegime.LUCRO_PRESUMIDO, TaxType.PIS, "0.6500", WithholdingMode.NEVER),
				rule(null, TaxRegime.LUCRO_REAL, TaxType.PIS, "1.6500", WithholdingMode.ALWAYS)));

		service.execute(command(pfTomador()));

		final NfseDocument saved = savedDocument();
		// The company is Lucro Presumido: its own regime row wins and the Lucro Real PIS row is ignored.
		assertThat(saved.getIssRate()).isEqualByComparingTo("3.5000");
		assertThat(saved.getWithholdings()).isEmpty();
	}

	@Test
	@DisplayName("Replaces the configured rate with a manual override that carries a justification")
	void ac3AManualOverrideWithJustificationReplacesTheConfiguredRate() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(standardRules());

		service.execute(command(pfTomador(), PlaceOfProvision.PROVIDER, "1.05", new BigDecimal("3.0"),
				"Beneficio fiscal municipal"));

		final NfseDocument saved = savedDocument();
		assertThat(saved.getIssRate()).isEqualByComparingTo("3.0");
		assertThat(saved.getIssAmount()).isEqualByComparingTo("30.00");
		assertThat(saved.isIssRateOverridden()).isTrue();
		assertThat(saved.getIssRateOverrideJustification()).isEqualTo("Beneficio fiscal municipal");
	}

	@Test
	@DisplayName("Keeps the configured ISS withholding behaviour when the rate is overridden")
	void ac3AnOverrideKeepsTheConfiguredIssWithholdingBehaviour() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(standardRules());

		service.execute(command(pjTomador(SP, true), PlaceOfProvision.PROVIDER, "1.05", new BigDecimal("3.0"), "j"));

		assertThat(savedDocument().getWithholdings()).filteredOn(w -> w.taxType() == TaxType.ISS)
				.singleElement().satisfies(w -> assertThat(w.amount()).isEqualByComparingTo("30.00"));
	}

	@Test
	@DisplayName("Rejects an override without justification")
	void ac3AnOverrideWithoutJustificationIsRejected() {
		assertThatThrownBy(() -> service.execute(command(pfTomador(), PlaceOfProvision.PROVIDER, "1.05",
				new BigDecimal("3.0"), null))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("justification");
		assertThatThrownBy(() -> service.execute(command(pfTomador(), PlaceOfProvision.PROVIDER, "1.05",
				new BigDecimal("3.0"), "   "))).isInstanceOf(BusinessRuleException.class);

		verify(nfseRepositoryPort, never()).save(any());
		verifyNoInteractions(allocateDocumentNumberUseCase);
	}

	@Test
	@DisplayName("Rejects an override outside the zero-to-one-hundred range")
	void ac3AnOverrideOutsideZeroToOneHundredIsRejected() {
		assertThatThrownBy(() -> service.execute(command(pfTomador(), PlaceOfProvision.PROVIDER, "1.05",
				new BigDecimal("120"), "j"))).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> service.execute(command(pfTomador(), PlaceOfProvision.PROVIDER, "1.05",
				new BigDecimal("-1"), "j"))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Lets a justified override cover a service with no configured rate")
	void ac3AJustifiedOverrideCoversAServiceWithNoConfiguredRate() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(List.of());

		service.execute(command(pfTomador(), PlaceOfProvision.PROVIDER, "1.05", new BigDecimal("4"), "j"));

		assertThat(savedDocument().getIssRate()).isEqualByComparingTo("4");
	}

	@Test
	@DisplayName("Rejects an RPS with no configured rate and no override")
	void ac3NoConfiguredRateAndNoOverrideIsRejected() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(List.of());

		assertThatThrownBy(() -> service.execute(command(pfTomador()))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("No ISS rate configured");
		verifyNoInteractions(allocateDocumentNumberUseCase);
	}

	@Test
	@DisplayName("Rejects withheld tax without the tomador's full address before any number is allocated")
	void ac2AWithheldTaxWithoutTheTomadorsFullAddressIsRejectedBeforeAnyNumberIsAllocated() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(standardRules());

		assertThatThrownBy(() -> service.execute(command(pjTomador(SP, false))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("full address");
		// the address is complete but the municipality is missing: still not a full address
		assertThatThrownBy(() -> service.execute(command(pjTomador(null, true))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("full address");

		verifyNoInteractions(allocateDocumentNumberUseCase);
		verify(nfseRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a service code outside the LC 116 list")
	void ac1ACodeOutsideTheLc116ListIsRejected() {
		assertThatThrownBy(() -> service.execute(command(pfTomador(), PlaceOfProvision.PROVIDER, "99.99", null, null)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("LC 116");
		assertThatThrownBy(() -> service.execute(command(pfTomador(), PlaceOfProvision.PROVIDER, "abc", null, null)))
				.isInstanceOf(BusinessRuleException.class);

		verify(nfseRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Accepts only the codes on the list when the municipality has its own list")
	void ac1AMunicipalityWithItsOwnListOnlyAcceptsTheCodesOnIt() {
		when(municipalServiceCodeRepositoryPort.hasServiceCodeList(SP)).thenReturn(true);
		when(municipalServiceCodeRepositoryPort.existsByMunicipalityAndServiceCode(SP, "01.05")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(command(pfTomador()))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("service list of municipality " + SP);
		verifyNoInteractions(serviceTaxRuleRepositoryPort);
	}

	@Test
	@DisplayName("Accepts a code that is on the municipal list")
	void ac1ACodeOnTheMunicipalListIsAccepted() {
		when(municipalServiceCodeRepositoryPort.hasServiceCodeList(SP)).thenReturn(true);
		when(municipalServiceCodeRepositoryPort.existsByMunicipalityAndServiceCode(SP, "01.05")).thenReturn(true);
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(standardRules());

		service.execute(command(pfTomador()));

		verify(nfseRepositoryPort).save(any());
	}

	@Test
	@DisplayName("Checks the list of the municipality to which ISS is due")
	void ac1TheListCheckedIsTheOneOfTheMunicipalityIssIsDueTo() {
		// place of provision = RECIPIENT: ISS is due to the tomador's municipality, so that municipality's list rules.
		when(municipalServiceCodeRepositoryPort.hasServiceCodeList(RIO)).thenReturn(true);
		when(municipalServiceCodeRepositoryPort.existsByMunicipalityAndServiceCode(RIO, "01.05")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(command(pjTomador(RIO, true), PlaceOfProvision.RECIPIENT, "1.05",
				null, null))).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("municipality " + RIO);
		verify(municipalServiceCodeRepositoryPort, never()).hasServiceCodeList(SP);
	}

	@Test
	@DisplayName("Resolves the rate in the tomador's municipality when the place of provision is the recipient")
	void recipientPlaceOfProvisionResolvesTheRateInTheTomadorsMunicipality() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", RIO)).thenReturn(List.of(
				rule(RIO, null, TaxType.ISS, "4.0000", WithholdingMode.NEVER)));

		service.execute(command(pjTomador(RIO, true), PlaceOfProvision.RECIPIENT, "1.05", null, null));

		final NfseDocument saved = savedDocument();
		assertThat(saved.getIssMunicipalityIbgeCode()).isEqualTo(RIO);
		assertThat(saved.getProviderMunicipalityIbgeCode()).isEqualTo(SP);
		assertThat(saved.getPlaceOfProvision()).isEqualTo(PlaceOfProvision.RECIPIENT);
		assertThat(saved.getIssRate()).isEqualByComparingTo("4.0000");
	}

	@Test
	@DisplayName("Requires the tomador's municipality when the place of provision is the recipient")
	void recipientPlaceOfProvisionRequiresTheTomadorsMunicipality() {
		assertThatThrownBy(() -> service.execute(command(pfTomador(), PlaceOfProvision.RECIPIENT, "1.05", null, null)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("municipalityIbgeCode");
	}

	@Test
	@DisplayName("Numbers the RPS from its own RPS series, not the NF-e one")
	void ac5NumbersTheRpsFromItsOwnRpsSeriesNotTheNfeOne() {
		when(serviceTaxRuleRepositoryPort.findCandidates("01.05", SP)).thenReturn(standardRules());

		service.execute(command(pfTomador()));

		final ArgumentCaptor<AllocateDocumentNumberCommand> captor = ArgumentCaptor
				.forClass(AllocateDocumentNumberCommand.class);
		verify(allocateDocumentNumberUseCase).execute(captor.capture());
		assertThat(captor.getValue().documentType()).isEqualTo(FiscalDocumentType.RPS);
		assertThat(captor.getValue().documentType()).isNotEqualTo(FiscalDocumentType.NFE);
		assertThat(captor.getValue().companyId()).isEqualTo(companyId);
		final NfseDocument saved = savedDocument();
		assertThat(saved.getRpsSeries()).isEqualTo("RPS1");
		assertThat(saved.getRpsNumber()).isEqualTo(42L);
	}

	@Test
	@DisplayName("Reports not found for an unknown provider company")
	void anUnknownProviderCompanyIsNotFound() {
		final UUID unknown = UUID.randomUUID();
		when(companyRepositoryPort.findById(CompanyId.of(unknown))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new IssueRpsCommand(unknown, SP, pfTomador(), "1.05",
				PlaceOfProvision.PROVIDER, BigDecimal.TEN, "x", null, null)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("Rejects an invalid tomador document")
	void anInvalidTomadorDocumentIsRejected() {
		final TomadorCommand bad = new TomadorCommand(null, "11111111111", PersonType.INDIVIDUAL, "X", null, null);

		assertThatThrownBy(() -> service.execute(command(bad))).isInstanceOf(RuntimeException.class);
		verify(allocateDocumentNumberUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Stores every rule table lookup with the service code in canonical form")
	void storesEveryRuleTableLookupWithTheServiceCodeInCanonicalForm() {
		when(serviceTaxRuleRepositoryPort.findCandidates(anyString(), eq(SP))).thenReturn(standardRules());

		service.execute(command(pfTomador(), PlaceOfProvision.PROVIDER, "0105", null, null));

		verify(serviceTaxRuleRepositoryPort).findCandidates("01.05", SP);
		assertThat(savedDocument().getServiceCode()).isEqualTo("01.05");
	}
}
