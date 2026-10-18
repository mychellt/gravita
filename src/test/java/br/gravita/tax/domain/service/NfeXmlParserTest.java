package br.gravita.tax.domain.service;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.usercases.tax.NfeXmlParser;
import br.gravita.core.usercases.tax.ParsedSupplierNfe;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NfeXmlParserTest {

	private final NfeXmlParser parser = new NfeXmlParser();

	@Test
	@DisplayName("Parses supplier name, items and taxes from an nfeProc XML")
	void parsesSupplierNameItemsAndTaxesFromANfeProcXml() {
		final ParsedSupplierNfe parsed = parser.parse(nfeProcXml().getBytes(StandardCharsets.UTF_8));

		assertThat(parsed.accessKey()).isEqualTo("35240111222333000181550010000012345123456789");
		assertThat(parsed.series()).isEqualTo("1");
		assertThat(parsed.number()).isEqualTo("12345");
		assertThat(parsed.supplierCnpj()).isEqualTo("11222333000181");
		assertThat(parsed.recipientDocument()).isEqualTo("11444777000161");
		assertThat(parsed.supplierName()).isEqualTo("Fornecedor Exemplo LTDA");
		assertThat(parsed.issuedAt()).isEqualTo(OffsetDateTime.parse("2026-01-15T10:00:00-03:00").toInstant());

		assertThat(parsed.items()).hasSize(2);
		final InboundNfeItem first = parsed.items().get(0);
		assertThat(first.supplierProductCode()).isEqualTo("SKU-001");
		assertThat(first.description()).isEqualTo("Parafuso Sextavado M8");
		assertThat(first.ncm()).isEqualTo("73181500");
		assertThat(first.cfop()).isEqualTo("5102");
		assertThat(first.unit()).isEqualTo("UN");
		assertThat(first.quantity()).isEqualByComparingTo("100.0000");
		assertThat(first.unitValue()).isEqualByComparingTo("1.5000");
		assertThat(first.totalValue()).isEqualByComparingTo("150.00");
		assertThat(first.icmsValue()).isEqualByComparingTo("27.00");
		assertThat(first.ipiValue()).isEqualByComparingTo("0.00");
		assertThat(first.pisValue()).isEqualByComparingTo("2.48");
		assertThat(first.cofinsValue()).isEqualByComparingTo("11.40");

		final InboundNfeItem second = parsed.items().get(1);
		assertThat(second.supplierProductCode()).isEqualTo("SKU-002");
		assertThat(second.quantity()).isEqualByComparingTo("10.0000");
		assertThat(first.barcode()).isEqualTo("7891234567895");
		assertThat(first.cest()).isEqualTo("2100100");
		assertThat(second.barcode()).isNull();
		assertThat(second.cest()).isNull();

		final var totals = parsed.totals();
		assertThat(totals.productsValue()).isEqualByComparingTo("250.00");
		assertThat(totals.freightValue()).isEqualByComparingTo("15.00");
		assertThat(totals.icmsValue()).isEqualByComparingTo("45.00");
		assertThat(totals.totalValue()).isEqualByComparingTo("265.00");
	}

	@Test
	@DisplayName("Falls back to the infNFe Id when the document has no protNFe")
	void fallsBackToInfNFeIdWhenTheDocumentHasNoProtNFe() {
		final ParsedSupplierNfe parsed = parser.parse(bareNfeXml().getBytes(StandardCharsets.UTF_8));

		assertThat(parsed.accessKey()).isEqualTo("35240111222333000181550010000012345123456789");
	}

	@Test
	@DisplayName("Takes the GTIN from cProd when the retailer writes SEM GTIN in cEAN and cProd is a valid GTIN")
	void takesTheGtinFromCProdWhenTheRetailerWritesSemGtin() {
		final ParsedSupplierNfe parsed = parser.parse(retailerNfeXml("7908597504167", "SEM GTIN", "SEM GTIN").getBytes(StandardCharsets.UTF_8));

		assertThat(parsed.items().get(0).barcode()).isEqualTo("7908597504167");
	}

	@Test
	@DisplayName("Ignores a cProd that only looks like a GTIN because its check digit is wrong")
	void ignoresACProdWithAWrongCheckDigit() {
		final ParsedSupplierNfe parsed = parser.parse(retailerNfeXml("7908597504160", "SEM GTIN", "SEM GTIN").getBytes(StandardCharsets.UTF_8));

		assertThat(parsed.items().get(0).barcode()).isNull();
	}

	@Test
	@DisplayName("Prefers the declared cEAN over a cProd that is also a GTIN")
	void prefersTheDeclaredCEan() {
		final ParsedSupplierNfe parsed = parser.parse(retailerNfeXml("7908597504167", "7891234567895", "SEM GTIN").getBytes(StandardCharsets.UTF_8));

		assertThat(parsed.items().get(0).barcode()).isEqualTo("7891234567895");
	}

	@Test
	@DisplayName("Has no GTIN when neither cEAN, cEANTrib nor cProd is one")
	void hasNoGtinWhenNothingIsOne() {
		final ParsedSupplierNfe parsed = parser.parse(retailerNfeXml("5663637", "SEM GTIN", "SEM GTIN").getBytes(StandardCharsets.UTF_8));

		assertThat(parsed.items().get(0).barcode()).isNull();
	}

	@Test
	@DisplayName("Rejects malformed XML as a business rule violation")
	void rejectsMalformedXmlAsABusinessRuleViolation() {
		assertThatThrownBy(() -> parser.parse("not xml at all".getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects XML missing required sections")
	void rejectsXmlMissingRequiredSections() {
		final String incomplete = """
				<?xml version="1.0" encoding="UTF-8"?>
				<NFe><infNFe Id="NFe1"></infNFe></NFe>
				""";
		assertThatThrownBy(() -> parser.parse(incomplete.getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("missing required sections");
	}

	@Test
	@DisplayName("Rejects XML with a DOCTYPE declaration to prevent XXE")
	void rejectsXmlWithADoctypeDeclarationToPreventXxe() {
		final String withDoctype = """
				<?xml version="1.0"?>
				<!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
				<NFe><infNFe Id="NFe35240111222333000181550010000012345123456789"></infNFe></NFe>
				""";
		assertThatThrownBy(() -> parser.parse(withDoctype.getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(BusinessRuleException.class);
	}

	private String retailerNfeXml(final String productCode, final String commercialEan, final String taxableEan) {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
				  <NFe>
				    <infNFe Id="NFe24251101438784005409650270002150481221083164" versao="4.00">
				      <ide><serie>27</serie><nNF>215048</nNF><dhEmi>2025-11-07T20:37:30-03:00</dhEmi></ide>
				      <emit><CNPJ>01438784005409</CNPJ><xNome>Varejista Exemplo</xNome></emit>
				      <dest><CPF>04993692463</CPF></dest>
				      <det nItem="1">
				        <prod>
				          <cProd>%s</cProd>
				          <cEAN>%s</cEAN>
				          <xProd>PISO BR BO AR IN 46X46 IPANEMA BG M2</xProd>
				          <NCM>69072200</NCM>
				          <CFOP>5102</CFOP>
				          <uCom>M2</uCom>
				          <qCom>2.3000</qCom>
				          <vUnCom>22.9000000000</vUnCom>
				          <vProd>52.67</vProd>
				          <cEANTrib>%s</cEANTrib>
				        </prod>
				        <imposto><ICMS><ICMS00><vICMS>9.91</vICMS></ICMS00></ICMS></imposto>
				      </det>
				      <total><ICMSTot><vProd>52.67</vProd><vNF>49.52</vNF></ICMSTot></total>
				    </infNFe>
				  </NFe>
				</nfeProc>
				""".formatted(productCode, commercialEan, taxableEan);
	}

	private String nfeProcXml() {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
				  <NFe>
				    <infNFe Id="NFe35240111222333000181550010000012345123456789" versao="4.00">
				      <ide>
				        <serie>1</serie>
				        <nNF>12345</nNF>
				        <dhEmi>2026-01-15T10:00:00-03:00</dhEmi>
				      </ide>
				      <emit>
				        <CNPJ>11222333000181</CNPJ>
				        <xNome>Fornecedor Exemplo LTDA</xNome>
				      </emit>
				      <dest>
				        <CNPJ>11444777000161</CNPJ>
				      </dest>
				      <det nItem="1">
				        <prod>
				          <cProd>SKU-001</cProd>
				          <cEAN>7891234567895</cEAN>
				          <xProd>Parafuso Sextavado M8</xProd>
				          <NCM>73181500</NCM>
				          <CEST>2100100</CEST>
				          <CFOP>5102</CFOP>
				          <uCom>UN</uCom>
				          <qCom>100.0000</qCom>
				          <vUnCom>1.5000</vUnCom>
				          <vProd>150.00</vProd>
				        </prod>
				        <imposto>
				          <ICMS><ICMS00><vICMS>27.00</vICMS></ICMS00></ICMS>
				          <PIS><PISAliq><vPIS>2.48</vPIS></PISAliq></PIS>
				          <COFINS><COFINSAliq><vCOFINS>11.40</vCOFINS></COFINSAliq></COFINS>
				        </imposto>
				      </det>
				      <det nItem="2">
				        <prod>
				          <cProd>SKU-002</cProd>
				          <cEAN>SEM GTIN</cEAN>
				          <xProd>Porca Sextavada M8</xProd>
				          <NCM>73181600</NCM>
				          <CFOP>5102</CFOP>
				          <uCom>UN</uCom>
				          <qCom>10.0000</qCom>
				          <vUnCom>10.0000</vUnCom>
				          <vProd>100.00</vProd>
				        </prod>
				        <imposto>
				          <ICMS><ICMS00><vICMS>18.00</vICMS></ICMS00></ICMS>
				          <PIS><PISAliq><vPIS>1.65</vPIS></PISAliq></PIS>
				          <COFINS><COFINSAliq><vCOFINS>7.60</vCOFINS></COFINSAliq></COFINS>
				        </imposto>
				      </det>
				      <total>
				        <ICMSTot>
				          <vProd>250.00</vProd>
				          <vFrete>15.00</vFrete>
				          <vSeg>0.00</vSeg>
				          <vDesc>0.00</vDesc>
				          <vOutro>0.00</vOutro>
				          <vICMS>45.00</vICMS>
				          <vIPI>0.00</vIPI>
				          <vPIS>4.13</vPIS>
				          <vCOFINS>19.00</vCOFINS>
				          <vNF>265.00</vNF>
				        </ICMSTot>
				      </total>
				    </infNFe>
				  </NFe>
				  <protNFe>
				    <infProt>
				      <chNFe>35240111222333000181550010000012345123456789</chNFe>
				    </infProt>
				  </protNFe>
				</nfeProc>
				""";
	}

	private String bareNfeXml() {
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<NFe xmlns="http://www.portalfiscal.inf.br/nfe">
				  <infNFe Id="NFe35240111222333000181550010000012345123456789" versao="4.00">
				    <ide>
				      <serie>1</serie>
				      <nNF>12345</nNF>
				      <dhEmi>2026-01-15T10:00:00-03:00</dhEmi>
				    </ide>
				    <emit>
				      <CNPJ>11222333000181</CNPJ>
				      <xNome>Fornecedor Exemplo LTDA</xNome>
				    </emit>
				    <det nItem="1">
				      <prod>
				        <cProd>SKU-001</cProd>
				        <xProd>Parafuso Sextavado M8</xProd>
				        <NCM>73181500</NCM>
				        <CFOP>5102</CFOP>
				        <uCom>UN</uCom>
				        <qCom>1.0000</qCom>
				        <vUnCom>10.0000</vUnCom>
				        <vProd>10.00</vProd>
				      </prod>
				      <imposto>
				        <ICMS><ICMS00><vICMS>1.80</vICMS></ICMS00></ICMS>
				      </imposto>
				    </det>
				    <total>
				      <ICMSTot>
				        <vProd>10.00</vProd>
				        <vICMS>1.80</vICMS>
				        <vNF>10.00</vNF>
				      </ICMSTot>
				    </total>
				  </infNFe>
				</NFe>
				""";
	}
}
