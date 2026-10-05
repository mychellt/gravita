package br.gravita.core.usercases.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class NfeXmlParser {

	public ParsedSupplierNfe parse(final byte[] xmlContent) {
		try {
			final org.w3c.dom.Document document = parseDocument(xmlContent);
			final XPath xpath = XPathFactory.newInstance().newXPath();

			final Node ide = node(xpath, document, "//*[local-name()='ide']");
			final Node emit = node(xpath, document, "//*[local-name()='emit']");
			final Node icmsTot = node(xpath, document, "//*[local-name()='total']/*[local-name()='ICMSTot']");
			final NodeList detNodes = nodeList(xpath, document, "//*[local-name()='det']");

			if (ide == null || emit == null || icmsTot == null || detNodes.getLength() == 0) {
				throw new BusinessRuleException("Supplier NFe XML is missing required sections (ide/emit/total/det)");
			}

			final String accessKey = extractAccessKey(xpath, document);
			final String series = text(xpath, ide, "*[local-name()='serie']");
			final String number = text(xpath, ide, "*[local-name()='nNF']");
			final Instant issuedAt = extractIssuedAt(xpath, ide);

			final String supplierCnpj = text(xpath, emit, "*[local-name()='CNPJ']");
			final String supplierName = text(xpath, emit, "*[local-name()='xNome']");

			final List<InboundNfeItem> items = extractItems(xpath, detNodes);
			final InboundNfeTotals totals = extractTotals(xpath, icmsTot);

			return new ParsedSupplierNfe(accessKey, series, number, supplierCnpj, supplierName, issuedAt, items,
					totals);
		} catch (final BusinessRuleException e) {
			throw e;
		} catch (final Exception e) {
			throw new BusinessRuleException("Unable to parse supplier NFe XML: " + e.getMessage(), e);
		}
	}

	private org.w3c.dom.Document parseDocument(final byte[] xmlContent) throws Exception {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		factory.setNamespaceAware(true);
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final org.w3c.dom.Document document = builder.parse(new InputSource(new ByteArrayInputStream(xmlContent)));
		document.getDocumentElement().normalize();
		return document;
	}

	private String extractAccessKey(final XPath xpath, final org.w3c.dom.Document document) throws Exception {
		final String fromProtocol = textOrNull(xpath, document, "//*[local-name()='protNFe']//*[local-name()='chNFe']");
		if (fromProtocol != null && !fromProtocol.isBlank()) {
			return fromProtocol.trim();
		}
		final String infNFeId = textOrNull(xpath, document, "//*[local-name()='infNFe']/@Id");
		if (infNFeId != null && infNFeId.length() >= 44) {
			return infNFeId.replaceFirst("^NFe", "").trim();
		}
		throw new BusinessRuleException("Supplier NFe XML has no access key (chNFe/infNFe@Id)");
	}

	private Instant extractIssuedAt(final XPath xpath, final Node ide) throws Exception {
		final String dhEmi = textOrNull(xpath, ide, "*[local-name()='dhEmi']");
		if (dhEmi != null && !dhEmi.isBlank()) {
			return OffsetDateTime.parse(dhEmi).toInstant();
		}
		final String issueDateText = textOrNull(xpath, ide, "*[local-name()='issueDateText']");
		if (issueDateText != null && !issueDateText.isBlank()) {
			return java.time.LocalDate.parse(issueDateText).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
		}
		throw new BusinessRuleException("Supplier NFe XML has no emission date (dhEmi/issueDateText)");
	}

	private List<InboundNfeItem> extractItems(final XPath xpath, final NodeList detNodes) throws Exception {
		final List<InboundNfeItem> items = new ArrayList<>();
		for (int i = 0; i < detNodes.getLength(); i++) {
			final Node det = detNodes.item(i);
			final Node prod = node(xpath, det, "*[local-name()='prod']");
			final Node taxNode = node(xpath, det, "*[local-name()='imposto']");
			if (prod == null) {
				throw new BusinessRuleException("Supplier NFe XML item #" + (i + 1) + " is missing <prod>");
			}

			items.add(new InboundNfeItem(
					text(xpath, prod, "*[local-name()='cProd']"),
					text(xpath, prod, "*[local-name()='xProd']"),
					textOrNull(xpath, prod, "*[local-name()='NCM']"),
					textOrNull(xpath, prod, "*[local-name()='CFOP']"),
					textOrNull(xpath, prod, "*[local-name()='uCom']"),
					decimal(xpath, prod, "*[local-name()='qCom']"),
					decimal(xpath, prod, "*[local-name()='vUnCom']"),
					decimal(xpath, prod, "*[local-name()='vProd']"),
					decimalOrZero(xpath, taxNode, ".//*[local-name()='ICMS']//*[local-name()='vICMS']"),
					decimalOrZero(xpath, taxNode, ".//*[local-name()='IPI']//*[local-name()='vIPI']"),
					decimalOrZero(xpath, taxNode, ".//*[local-name()='PIS']//*[local-name()='vPIS']"),
					decimalOrZero(xpath, taxNode, ".//*[local-name()='COFINS']//*[local-name()='vCOFINS']")));
		}
		return items;
	}

	private InboundNfeTotals extractTotals(final XPath xpath, final Node icmsTot) throws Exception {
		return new InboundNfeTotals(
				decimal(xpath, icmsTot, "*[local-name()='vProd']"),
				decimalOrZero(xpath, icmsTot, "*[local-name()='vFrete']"),
				decimalOrZero(xpath, icmsTot, "*[local-name()='vSeg']"),
				decimalOrZero(xpath, icmsTot, "*[local-name()='vDesc']"),
				decimalOrZero(xpath, icmsTot, "*[local-name()='vOutro']"),
				decimalOrZero(xpath, icmsTot, "*[local-name()='vICMS']"),
				decimalOrZero(xpath, icmsTot, "*[local-name()='vIPI']"),
				decimalOrZero(xpath, icmsTot, "*[local-name()='vPIS']"),
				decimalOrZero(xpath, icmsTot, "*[local-name()='vCOFINS']"),
				decimal(xpath, icmsTot, "*[local-name()='vNF']"));
	}

	private Node node(final XPath xpath, final Object context, final String expression) throws Exception {
		return (Node) xpath.evaluate(expression, context, XPathConstants.NODE);
	}

	private NodeList nodeList(final XPath xpath, final Object context, final String expression) throws Exception {
		return (NodeList) xpath.evaluate(expression, context, XPathConstants.NODESET);
	}

	private String text(final XPath xpath, final Object context, final String expression) throws Exception {
		final String value = textOrNull(xpath, context, expression);
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException("Supplier NFe XML is missing required field: " + expression);
		}
		return value;
	}

	private String textOrNull(final XPath xpath, final Object context, final String expression) throws Exception {
		final String value = (String) xpath.evaluate(expression, context, XPathConstants.STRING);
		return value == null || value.isEmpty() ? null : value;
	}

	private BigDecimal decimal(final XPath xpath, final Object context, final String expression) throws Exception {
		return new BigDecimal(text(xpath, context, expression));
	}

	private BigDecimal decimalOrZero(final XPath xpath, final Object context, final String expression) throws Exception {
		if (context == null) {
			return BigDecimal.ZERO;
		}
		final String value = textOrNull(xpath, context, expression);
		return value == null ? BigDecimal.ZERO : new BigDecimal(value);
	}
}
