package br.gravita.purchasing.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ImportSupplierNfeAtReceivingEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@Autowired
	private PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;

	private final UUID productId = UUID.randomUUID();

	@Test
	void importingTheSupplierXmlCompletesConferenceAndReturnsTheResult() throws Exception {
		PurchaseOrder order = seedOpenOrder();
		PurchaseReceipt receipt = seedPendingReceipt(order.getId());
		MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfe.xml", "text/xml",
				nfeXml().getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart(
						"/api/purchasing/orders/" + order.getId().value() + "/receipts/" + receipt.getId().value()
								+ "/import-nfe")
						.file(xmlFile)
						.param("companyId", UUID.randomUUID().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lines").isArray())
				.andExpect(jsonPath("$.orderedValue").value(50.0))
				.andExpect(jsonPath("$.invoicedValue").value(10.0))
				.andExpect(jsonPath("$.hasDivergences").value(true));
	}

	@Test
	void importingAgainstAnUnknownReceiptIsRejectedWith404() throws Exception {
		PurchaseOrder order = seedOpenOrder();
		MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfe.xml", "text/xml",
				nfeXml().getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart(
						"/api/purchasing/orders/" + order.getId().value() + "/receipts/" + UUID.randomUUID()
								+ "/import-nfe")
						.file(xmlFile)
						.param("companyId", UUID.randomUUID().toString()))
				.andExpect(status().isNotFound());
	}

	private PurchaseOrder seedOpenOrder() {
		var order = PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()),
				PurchaseRequestId.of(UUID.randomUUID()), null, SupplierId.of(UUID.randomUUID()),
				List.of(new PurchaseOrderItem(productId, BigDecimal.TEN, new BigDecimal("5.00"))), false);
		return purchaseOrderRepositoryPort.save(order);
	}

	private PurchaseReceipt seedPendingReceipt(PurchaseOrderId orderId) {
		var receipt = PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
				List.of(new PurchaseReceiptItem(productId, BigDecimal.TEN, BigDecimal.TEN)));
		return purchaseReceiptRepositoryPort.save(receipt);
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
