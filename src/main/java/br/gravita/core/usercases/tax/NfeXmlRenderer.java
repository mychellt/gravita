package br.gravita.core.usercases.tax;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeItem;
import java.nio.charset.StandardCharsets;

/**
 * Serializes an authorized {@link NfeDocument} to XML for storage/e-mail
 * (UC-M2-03, AC6). This is a simplified, gravita-internal representation of
 * the document's fiscal data (access key, issuer, recipient, items, totals,
 * protocol) - not a byte-for-byte implementation of SEFAZ's full "NFe"
 * layout-4.00 XSD, which is out of scope here (see
 * {@code SefazUfSubmissionAdapter#manifest} for a similar, explicitly-scoped
 * gap). Element names still follow the official schema's own tags
 * ({@code ide}, {@code emit}, {@code dest}, {@code det}, {@code total}) so
 * the output is at least structurally familiar.
 */
final class NfeXmlRenderer {

	private NfeXmlRenderer() {
	}

	static byte[] render(final NfeDocument document, final Company company) {
		final StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		xml.append("<NFe><infNFe Id=\"NFe").append(escape(document.getAccessKey())).append("\">");
		xml.append("<ide>");
		xml.append("<cUF>").append(escape(company.getState())).append("</cUF>");
		xml.append("<serie>").append(escape(document.getDocumentSeries())).append("</serie>");
		xml.append("<nNF>").append(document.getDocumentNumber()).append("</nNF>");
		xml.append("</ide>");
		xml.append("<emit>");
		xml.append("<CNPJ>").append(escape(company.getCnpj().number())).append("</CNPJ>");
		xml.append("</emit>");
		xml.append("<dest>");
		xml.append("<xNome>").append(escape(document.getRecipient().name())).append("</xNome>");
		xml.append("<doc>").append(escape(document.getRecipient().document().number())).append("</doc>");
		xml.append("</dest>");
		for (final NfeItem item : document.getItems()) {
			xml.append("<det>");
			xml.append("<prod>");
			xml.append("<xProd>").append(escape(item.description())).append("</xProd>");
			xml.append("<qCom>").append(item.quantity()).append("</qCom>");
			xml.append("<vUnCom>").append(item.unitPrice()).append("</vUnCom>");
			xml.append("<vProd>").append(item.lineTotal()).append("</vProd>");
			xml.append("</prod>");
			xml.append("</det>");
		}
		xml.append("<total><ICMSTot>");
		xml.append("<vNF>").append(document.getDocumentTotal()).append("</vNF>");
		xml.append("</ICMSTot></total>");
		xml.append("<protNFe><infProt><nProt>").append(escape(document.getSefazProtocol()))
				.append("</nProt></infProt></protNFe>");
		xml.append("</infNFe></NFe>");
		return xml.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static String escape(final String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
