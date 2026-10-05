package br.gravita.adapters.outbound.rendering.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseStandard;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.NfseWithholding;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TomadorAddress;
import br.gravita.core.ports.inbound.tax.NfseTransmissionResult;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

class GuidedManualUploadAdapterTest {

	private static final String SP = "3550308";

	private final CompanyRepositoryPort companyRepositoryPort = mock(CompanyRepositoryPort.class);
	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private GuidedManualUploadAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new GuidedManualUploadAdapter(companyRepositoryPort);
		when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(Company.of(companyId, "Acme Ltda",
				Document.cnpj("11222333000181"), "123456789", "987654", "6201500", TaxRegime.SIMPLES_NACIONAL, true,
				SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP", "nfse@example.com", "11999999999", null,
				null)));
	}

	private NfseDocument draft(NfseTomador tomador, List<NfseWithholding> withholdings, String discrimination) {
		return NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), companyId, SP, tomador, ServiceCode.of("1.05"),
				PlaceOfProvision.PROVIDER, SP, new BigDecimal("1000.00"), new BigDecimal("5.0000"),
				new BigDecimal("50.00"), null, withholdings, discrimination, "RPS", 12L,
				Instant.parse("2026-10-01T15:00:00Z")).convertToNfse("1", 3L, Instant.now());
	}

	private static MunicipalityIntegration integration(NfseStandard standard, boolean homologated,
			List<String> requiredFields) {
		return MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()), SP, standard,
				homologated ? "2.04" : null, homologated ? "https://nfse.example/ws" : null, CertificateType.A1,
				requiredFields, homologated);
	}

	private static org.w3c.dom.Document parse(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	private static String text(org.w3c.dom.Document doc, String tag) {
		NodeList nodes = doc.getElementsByTagNameNS("*", tag);
		return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
	}

	@Test
	@DisplayName("Renders a well-formed ABRASF XML with the provider, tomador and service data")
	void rendersAWellFormedAbrasfXmlWithTheProviderTomadorAndServiceData() throws Exception {
		NfseTomador tomador = NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Maria & Filhos <ME>", SP,
				new TomadorAddress("Rua A", "10", null, "Centro", "01001000", "SP"));

		NfseTransmissionResult.GuidedManualUpload result = adapter.generate(
				draft(tomador, List.of(), "Consultoria \"premium\""), integration(NfseStandard.ABRASF, false, List.of()));

		org.w3c.dom.Document xml = parse(result.xml());
		assertThat(xml.getDocumentElement().getLocalName()).isEqualTo("GerarNfseEnvio");
		assertThat(text(xml, "Cnpj")).isEqualTo("11222333000181");
		assertThat(text(xml, "InscricaoMunicipal")).isEqualTo("987654");
		assertThat(text(xml, "Cpf")).isEqualTo("52998224725");
		assertThat(text(xml, "RazaoSocial")).isEqualTo("Maria & Filhos <ME>");
		assertThat(text(xml, "Discriminacao")).isEqualTo("Consultoria \"premium\"");
		assertThat(text(xml, "ValorServicos")).isEqualTo("1000.00");
		assertThat(text(xml, "ValorIss")).isEqualTo("50.00");
		assertThat(text(xml, "Aliquota")).isEqualTo("0.0500");
		assertThat(text(xml, "ItemListaServico")).isEqualTo("01.05");
		assertThat(text(xml, "CodigoMunicipio")).isEqualTo(SP);
		assertThat(text(xml, "Numero")).isEqualTo("12");
		assertThat(text(xml, "Competencia")).isEqualTo("2026-10-01");
		assertThat(text(xml, "OptanteSimplesNacional")).isEqualTo("1");
		assertThat(text(xml, "IssRetido")).isEqualTo("2");
	}

	@Test
	@DisplayName("Identifies a company tomador by CNPJ and flags withheld ISS")
	void aCompanyTomadorIsIdentifiedByCnpjAndWithheldIssIsFlagged() throws Exception {
		NfseTomador tomador = NfseTomador.of(null, "11222333000181", PersonType.COMPANY, "Tomador SA", SP,
				new TomadorAddress("Rua A", "10", "Sala 2", "Centro", "01001000", "SP"));
		List<NfseWithholding> withholdings = List.of(new NfseWithholding(TaxType.ISS, new BigDecimal("1000.00"),
				new BigDecimal("5.0000"), new BigDecimal("50.00")));

		org.w3c.dom.Document xml = parse(adapter.generate(draft(tomador, withholdings, "Servico"),
				integration(NfseStandard.BETHA, false, List.of())).xml());

		assertThat(text(xml, "IssRetido")).isEqualTo("1");
		assertThat(text(xml, "Cpf")).isNull();
		assertThat(text(xml, "Complemento")).isEqualTo("Sala 2");
		assertThat(xml.getElementsByTagNameNS("*", "Cnpj").getLength()).isEqualTo(2);
	}

	@Test
	@DisplayName("Produces guided instructions for every standard, naming the standard certificate and required fields")
	void producesGuidedInstructionsForEveryStandardNamingTheStandardCertificateAndRequiredFields() {
		NfseTomador tomador = NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null);
		NfseDocument draft = draft(tomador, List.of(), "Servico");

		for (NfseStandard standard : NfseStandard.values()) {
			NfseTransmissionResult.GuidedManualUpload result = adapter.generate(draft,
					integration(standard, false, List.of("inscricaoMunicipal", "codigoTributacao")));

			assertThat(result.xml()).startsWith("<?xml");
			assertThat(result.instructions()).contains(standard.name(), SP, "A1", "inscricaoMunicipal, codigoTributacao")
					.contains("rps-12.xml");
		}
	}

	@Test
	@DisplayName("Rejects an unknown provider company as a business rule violation")
	void anUnknownProviderCompanyIsABusinessRuleViolation() {
		when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.empty());
		NfseTomador tomador = NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null);

		assertThatThrownBy(() -> adapter.generate(draft(tomador, List.of(), "Servico"),
				integration(NfseStandard.ABRASF, false, List.of()))).isInstanceOf(BusinessRuleException.class);
	}
}
