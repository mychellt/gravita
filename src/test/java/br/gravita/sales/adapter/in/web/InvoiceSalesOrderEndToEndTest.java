package br.gravita.sales.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesInvoice;
import br.gravita.core.domain.sales.SalesInvoiceId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesInvoiceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.sales.GenerateAccountsReceivablePort;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class InvoiceSalesOrderEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Autowired
	private SalesInvoiceRepositoryPort salesInvoiceRepositoryPort;

	@Autowired
	private ProductRepositoryPort productRepositoryPort;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private IssueFiscalDocumentPort issueFiscalDocumentPort;

	@MockitoBean
	private GenerateAccountsReceivablePort generateAccountsReceivablePort;

	@Test
	@DisplayName("Invoicing an approved order issues the NF-e and moves the order to INVOICED")
	void invoicingAnApprovedOrderIssuesTheNfeAndTransitionsTheOrderToInvoiced() throws Exception {
		UUID productId = seedProduct(ProductType.SIMPLE);
		SalesOrder order = persistOrder(SalesOrderStatus.APPROVED, productId, BigDecimal.ONE,
				new BigDecimal("20.00"));
		FiscalDocumentRef nfeRef = new FiscalDocumentRef(FiscalDocumentType.NFE, UUID.randomUUID());
		when(issueFiscalDocumentPort.issueForProducts(any())).thenReturn(nfeRef);

		MvcResult result = mockMvc.perform(post("/api/sales/orders/" + order.getId().value() + "/invoice"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderId").value(order.getId().value().toString()))
				.andExpect(jsonPath("$.status").value("ISSUED"))
				.andExpect(jsonPath("$.fiscalDocuments[0].type").value("NFE"))
				.andExpect(jsonPath("$.fiscalDocuments[0].documentId").value(nfeRef.documentId().toString()))
				.andReturn();

		SalesOrder reloaded = salesOrderRepositoryPort.findById(order.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(SalesOrderStatus.INVOICED);

		UUID invoiceId = UUID
				.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
		SalesInvoice invoice = salesInvoiceRepositoryPort.findById(SalesInvoiceId.of(invoiceId)).orElseThrow();
		assertThat(invoice.getOrderId()).isEqualTo(order.getId());
		assertThat(invoice.getFiscalDocuments()).containsExactly(nfeRef);

		verify(generateAccountsReceivablePort).generate(any());
		verify(issueFiscalDocumentPort, never()).issueForServices(any());
	}

	@Test
	@DisplayName("Invoicing a draft order is rejected")
	void rejectsInvoicingADraftOrder() throws Exception {
		UUID productId = seedProduct(ProductType.SIMPLE);
		SalesOrder order = persistOrder(SalesOrderStatus.DRAFT, productId, BigDecimal.ONE, new BigDecimal("20.00"));

		mockMvc.perform(post("/api/sales/orders/" + order.getId().value() + "/invoice"))
				.andExpect(status().isBadRequest());

		verify(issueFiscalDocumentPort, never()).issueForProducts(any());
	}

	@Test
	@DisplayName("Invoicing an order that does not exist returns 404")
	void anOrderThatDoesNotExistIsRejectedWith404() throws Exception {
		mockMvc.perform(post("/api/sales/orders/" + UUID.randomUUID() + "/invoice"))
				.andExpect(status().isNotFound());
	}

	private SalesOrder persistOrder(SalesOrderStatus status, UUID productId, BigDecimal quantity,
			BigDecimal unitPrice) {
		SalesOrder order = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(),
				List.of(new SalesOrderItem(productId, quantity, unitPrice, BigDecimal.ZERO)), status,
				status == SalesOrderStatus.DRAFT ? null : UUID.randomUUID(), null);
		return salesOrderRepositoryPort.save(order);
	}

	private UUID seedProduct(ProductType type) {
		ProductDomain product = ProductDomain.builder().type(type).status(ProductStatus.ACTIVE)
				.internalCode("SKU-" + UUID.randomUUID()).build();
		product.setId(UUID.randomUUID());
		return productRepositoryPort.save(product).getId();
	}
}
