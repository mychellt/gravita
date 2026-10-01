package br.gravita.adapters.outbound.rendering.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.NfseWithholding;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TomadorAddress;
import br.gravita.core.ports.inbound.tax.NfseTransmissionResult;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.tax.GenerateGuidedManualUploadPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Non-homologated fallback (UC-M4-04): renders the NFSe as an ABRASF-layout {@code GerarNfseEnvio} XML the user can
 * upload on the municipality's own portal, with step-by-step instructions.
 *
 * <p>Scope: the layout follows ABRASF 2.04's {@code InfDeclaracaoPrestacaoServico} structure and is used for every
 * {@code NfseStandard}; the standard and version only appear in the instructions. Per-standard layouts (NFS-e
 * Nacional, ISS.net, Betha) and the full XSD (digital signature, optional blocks) are not implemented - like the M2
 * {@code NfeXmlRenderer}, this is a faithful-in-shape representation, not a schema-validated one.
 */
@Component
class GuidedManualUploadAdapter implements GenerateGuidedManualUploadPort {

	private static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private final CompanyRepositoryPort companyRepositoryPort;

	GuidedManualUploadAdapter(CompanyRepositoryPort companyRepositoryPort) {
		this.companyRepositoryPort = companyRepositoryPort;
	}

	@Override
	public NfseTransmissionResult.GuidedManualUpload generate(NfseDocument document,
			MunicipalityIntegration integration) {
		Company provider = companyRepositoryPort.findById(document.getProviderCompanyId())
				.orElseThrow(() -> new BusinessRuleException(
						"Provider company not found: " + document.getProviderCompanyId().value()));
		return new NfseTransmissionResult.GuidedManualUpload(renderXml(document, provider),
				instructions(document, integration));
	}

	private static String renderXml(NfseDocument document, Company provider) {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		xml.append("<GerarNfseEnvio xmlns=\"http://www.abrasf.org.br/nfse.xsd\">");
		xml.append("<Rps><InfDeclaracaoPrestacaoServico Id=\"rps").append(document.getRpsNumber()).append("\">");

		xml.append("<Rps><IdentificacaoRps>");
		element(xml, "Numero", String.valueOf(document.getRpsNumber()));
		element(xml, "Serie", document.getRpsSeries());
		element(xml, "Tipo", "1");
		xml.append("</IdentificacaoRps>");
		element(xml, "DataEmissao", DATE_TIME.format(document.getCreatedAt().atZone(BRAZIL)));
		element(xml, "Status", "1");
		xml.append("</Rps>");
		element(xml, "Competencia", DATE.format(document.getCreatedAt().atZone(BRAZIL)));

		xml.append("<Servico><Valores>");
		element(xml, "ValorServicos", money(document.getServiceAmount()));
		element(xml, "ValorIss", money(document.getIssAmount()));
		// ABRASF 2.04 carries the rate as a fraction (0.0500), not a percentage.
		element(xml, "Aliquota", document.getIssRate().movePointLeft(2).setScale(4, RoundingMode.HALF_UP)
				.toPlainString());
		xml.append("</Valores>");
		element(xml, "IssRetido", isIssWithheld(document) ? "1" : "2");
		element(xml, "ItemListaServico", document.getServiceCode());
		element(xml, "Discriminacao", document.getDiscrimination());
		element(xml, "CodigoMunicipio", document.getIssMunicipalityIbgeCode());
		element(xml, "ExigibilidadeISS", "1");
		xml.append("</Servico>");

		xml.append("<Prestador><CpfCnpj>");
		element(xml, "Cnpj", provider.getCnpj().number());
		xml.append("</CpfCnpj>");
		element(xml, "InscricaoMunicipal", provider.getIm());
		xml.append("</Prestador>");

		renderTomador(xml, document.getTomador());

		element(xml, "OptanteSimplesNacional", provider.isSimplesOptante() ? "1" : "2");
		element(xml, "IncentivoFiscal", "2");
		xml.append("</InfDeclaracaoPrestacaoServico></Rps></GerarNfseEnvio>");
		return xml.toString();
	}

	private static void renderTomador(StringBuilder xml, NfseTomador tomador) {
		xml.append("<TomadorServico><IdentificacaoTomador><CpfCnpj>");
		element(xml, tomador.isCompany() ? "Cnpj" : "Cpf", tomador.document().number());
		xml.append("</CpfCnpj></IdentificacaoTomador>");
		element(xml, "RazaoSocial", tomador.name());
		TomadorAddress address = tomador.address();
		if (address != null) {
			xml.append("<Endereco>");
			element(xml, "Endereco", address.street());
			element(xml, "Numero", address.number());
			if (address.complement() != null && !address.complement().isBlank()) {
				element(xml, "Complemento", address.complement());
			}
			element(xml, "Bairro", address.neighborhood());
			if (tomador.municipalityIbgeCode() != null) {
				element(xml, "CodigoMunicipio", tomador.municipalityIbgeCode());
			}
			element(xml, "Uf", address.state());
			element(xml, "Cep", address.zipCode());
			xml.append("</Endereco>");
		}
		xml.append("</TomadorServico>");
	}

	private static boolean isIssWithheld(NfseDocument document) {
		return document.getWithholdings().stream().map(NfseWithholding::taxType).anyMatch(TaxType.ISS::equals);
	}

	private static String instructions(NfseDocument document, MunicipalityIntegration integration) {
		StringBuilder text = new StringBuilder();
		text.append("Municipality ").append(integration.getIbgeCode())
				.append(" has no homologated NFSe integration, so this NFSe was not transmitted automatically. ")
				.append("Issue it manually on the municipality's own portal:\n");
		text.append("1. Save the generated XML as a file (e.g. rps-").append(document.getRpsNumber()).append(".xml).\n");
		text.append("2. Sign in to the municipality's NFSe portal with the provider's ")
				.append(integration.getRequiredCertificateType())
				.append(" digital certificate.\n");
		text.append("3. Choose the XML/batch import option (the municipality's standard is ")
				.append(integration.getStandard());
		if (integration.getVersion() != null && !integration.getVersion().isBlank()) {
			text.append(" ").append(integration.getVersion());
		}
		text.append(") and upload the file.\n");
		if (!integration.getRequiredFields().isEmpty()) {
			text.append("4. This municipality also requires: ")
					.append(String.join(", ", integration.getRequiredFields())).append(".\n");
		}
		text.append("The NFSe stays a DRAFT here until it is authorized by the municipality.");
		return text.toString();
	}

	private static void element(StringBuilder xml, String name, String value) {
		xml.append('<').append(name).append('>').append(escape(value)).append("</").append(name).append('>');
	}

	private static String money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
