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

	public ParsedSupplierNfe parse(byte[] xmlContent) {
		try {
			org.w3c.dom.Document document = parseDocument(xmlContent);
			XPath xpath = XPathFactory.newInstance().newXPath();

			Node ide = node(xpath, document, "//*[local-name()='ide']");
			Node emit = node(xpath, document, "//*[local-name()='emit']");
			Node icmsTot = node(xpath, document, "//*[local-name()='total']/*[local-name()='ICMSTot']");
			NodeList detNodes = nodeList(xpath, document, "//*[local-name()='det']");

			if (ide == null || emit == null || icmsTot == null || detNodes.getLength() == 0) {
				throw new BusinessRuleException("Supplier NFe XML is missing required sections (ide/emit/total/det)");
			}

			String accessKey = extractAccessKey(xpath, document);
			String series = text(xpath, ide, "*[local-name()='serie']");
			String number = text(xpath, ide, "*[local-name()='nNF']");
			Instant issuedAt = extractIssuedAt(xpath, ide);

			String supplierCnpj = text(xpath, emit, "*[local-name()='CNPJ']");
			String supplierName = text(xpath, emit, "*[local-name()='xNome']");

			List<InboundNfeItem> items = extractItems(xpath, detNodes);
			InboundNfeTotals totals = extractTotals(xpath, icmsTot);

			return new ParsedSupplierNfe(accessKey, series, number, supplierCnpj, supplierName, issuedAt, items,
					totals);
		} catch (BusinessRuleException e) {
			throw e;
		} catch (Exception e) {
			throw new BusinessRuleException("Unable to parse supplier NFe XML: " + e.getMessage(), e);
		}
	}

	private org.w3c.dom.Document parseDocument(byte[] xmlContent) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		org.w3c.dom.Document document = builder.parse(new InputSource(new ByteArrayInputStream(xmlContent)));
		document.getDocumentElement().normalize();
		return document;
	}

	private String extractAccessKey(XPath xpath, org.w3c.dom.Document document) throws Exception {
		String fromProtocol = textOrNull(xpath, document, "//*[local-name()='protNFe']//*[local-name()='chNFe']");
		if (fromProtocol != null && !fromProtocol.isBlank()) {
			return fromProtocol.trim();
		}
		String infNFeId = textOrNull(xpath, document, "//*[local-name()='infNFe']/@Id");
		if (infNFeId != null && infNFeId.length() >= 44) {
			return infNFeId.replaceFirst("^NFe", "").trim();
		}
		throw new BusinessRuleException("Supplier NFe XML has no access key (chNFe/infNFe@Id)");
	}

	private Instant extractIssuedAt(XPath xpath, Node ide) throws Exception {
		String dhEmi = textOrNull(xpath, ide, "*[local-name()='dhEmi']");
		if (dhEmi != null && !dhEmi.isBlank()) {
			return OffsetDateTime.parse(dhEmi).toInstant();
		}
		String dEmi = textOrNull(xpath, ide, "*[local-name()='dEmi']");
		if (dEmi != null && !dEmi.isBlank()) {
			return java.time.LocalDate.parse(dEmi).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
		}
		throw new BusinessRuleException("Supplier NFe XML has no emission date (dhEmi/dEmi)");
	}

	private List<InboundNfeItem> extractItems(XPath xpath, NodeList detNodes) throws Exception {
		List<InboundNfeItem> items = new ArrayList<>();
		for (int i = 0; i < detNodes.getLength(); i++) {
			Node det = detNodes.item(i);
			Node prod = node(xpath, det, "*[local-name()='prod']");
			Node imposto = node(xpath, det, "*[local-name()='imposto']");
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
					decimalOrZero(xpath, imposto, ".//*[local-name()='ICMS']//*[local-name()='vICMS']"),
					decimalOrZero(xpath, imposto, ".//*[local-name()='IPI']//*[local-name()='vIPI']"),
					decimalOrZero(xpath, imposto, ".//*[local-name()='PIS']//*[local-name()='vPIS']"),
					decimalOrZero(xpath, imposto, ".//*[local-name()='COFINS']//*[local-name()='vCOFINS']")));
		}
		return items;
	}

	private InboundNfeTotals extractTotals(XPath xpath, Node icmsTot) throws Exception {
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

	private Node node(XPath xpath, Object context, String expression) throws Exception {
		return (Node) xpath.evaluate(expression, context, XPathConstants.NODE);
	}

	private NodeList nodeList(XPath xpath, Object context, String expression) throws Exception {
		return (NodeList) xpath.evaluate(expression, context, XPathConstants.NODESET);
	}

	private String text(XPath xpath, Object context, String expression) throws Exception {
		String value = textOrNull(xpath, context, expression);
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException("Supplier NFe XML is missing required field: " + expression);
		}
		return value;
	}

	private String textOrNull(XPath xpath, Object context, String expression) throws Exception {
		String value = (String) xpath.evaluate(expression, context, XPathConstants.STRING);
		return value == null || value.isEmpty() ? null : value;
	}

	private BigDecimal decimal(XPath xpath, Object context, String expression) throws Exception {
		return new BigDecimal(text(xpath, context, expression));
	}

	private BigDecimal decimalOrZero(XPath xpath, Object context, String expression) throws Exception {
		if (context == null) {
			return BigDecimal.ZERO;
		}
		String value = textOrNull(xpath, context, expression);
		return value == null ? BigDecimal.ZERO : new BigDecimal(value);
	}
}
