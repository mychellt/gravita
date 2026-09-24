package br.gravita.tax.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ImportSupplierNfeXmlEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void uploadingASupplierXmlCreatesAPendingConferenceInboundNfe() throws Exception {
		MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfe.xml", "text/xml",
				nfeXml().getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/nfe/inbound/import-xml")
						.file(xmlFile)
						.param("companyId", UUID.randomUUID().toString()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.accessKey").value("35240111222333000181550010000012345123456789"))
				.andExpect(jsonPath("$.supplierName").value("Fornecedor Exemplo LTDA"))
				.andExpect(jsonPath("$.status").value("PENDING_CONFERENCE"));
	}

	@Test
	void uploadingAMalformedXmlIsRejectedWith400() throws Exception {
		MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfe.xml", "text/xml",
				"not xml".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/nfe/inbound/import-xml")
						.file(xmlFile)
						.param("companyId", UUID.randomUUID().toString()))
				.andExpect(status().isBadRequest());
	}

	private String nfeXml() {
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
