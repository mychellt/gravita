package br.gravita.tax.application.service;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlCommand;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.usercases.tax.ImportSupplierNfeXmlService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportSupplierNfeXmlServiceTest {

	@Mock
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Mock
	private XmlObjectStoragePort xmlObjectStoragePort;

	private ImportSupplierNfeXmlService service;

	@BeforeEach
	void setUp() {
		service = new ImportSupplierNfeXmlService(inboundNfeRepositoryPort, xmlObjectStoragePort);
	}

	@Test
	@DisplayName("Stores the XML before persisting and returns an inbound NF-e pending conference")
	void storesTheXmlBeforePersistingAndReturnsAPendingConferenceInboundNfe() {
		CompanyId companyId = CompanyId.of(UUID.randomUUID());
		byte[] xml = nfeXml().getBytes(StandardCharsets.UTF_8);
		when(xmlObjectStoragePort.store(eq(companyId), eq(xml))).thenReturn("xml-object-ref-1");
		when(inboundNfeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		InboundNfe result = service.execute(new ImportSupplierNfeXmlCommand(companyId, xml));

		assertThat(result.getStatus()).isEqualTo(InboundNfeStatus.PENDING_CONFERENCE);
		assertThat(result.getXmlStorageRef()).isEqualTo("xml-object-ref-1");
		assertThat(result.getCompanyId()).isEqualTo(companyId);
		assertThat(result.getSupplierName()).isEqualTo("Fornecedor Exemplo LTDA");
		assertThat(result.getItems()).hasSize(1);

		ArgumentCaptor<InboundNfe> savedCaptor = ArgumentCaptor.forClass(InboundNfe.class);
		org.mockito.Mockito.verify(inboundNfeRepositoryPort).save(savedCaptor.capture());
		assertThat(savedCaptor.getValue().getXmlStorageRef()).isEqualTo("xml-object-ref-1");
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
