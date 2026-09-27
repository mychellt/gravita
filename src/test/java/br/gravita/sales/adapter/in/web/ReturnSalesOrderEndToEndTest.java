package br.gravita.sales.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import br.gravita.core.ports.outbound.sales.AdjustReceivableForReturnPort;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort;
import br.gravita.core.ports.outbound.sales.RegisterStockEntryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ReturnSalesOrderEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Autowired
	private SalesInvoiceRepositoryPort salesInvoiceRepositoryPort;

	@Autowired
	private ProductRepositoryPort productRepositoryPort;

	@MockitoBean
	private IssueFiscalDocumentPort issueFiscalDocumentPort;

	@MockitoBean
	private RegisterStockEntryPort registerStockEntryPort;

	@MockitoBean
	private AdjustReceivableForReturnPort adjustReceivableForReturnPort;

	@Test
	void returningAnInvoicedOrderRevertsStockIssuesTheReturnNfeAndAdjustsTheReceivable() throws Exception {
		UUID productId = seedProduct();
		FiscalDocumentRef originalNfeRef = new FiscalDocumentRef(FiscalDocumentType.NFE, UUID.randomUUID());
		SalesOrder order = persistInvoicedOrder(productId, BigDecimal.TEN, new BigDecimal("5.00"), originalNfeRef);
		FiscalDocumentRef returnNfeRef = new FiscalDocumentRef(FiscalDocumentType.NFE, UUID.randomUUID());
		when(issueFiscalDocumentPort.issueForReturn(any())).thenReturn(returnNfeRef);

		mockMvc.perform(post("/api/sales/orders/" + order.getId().value() + "/return")
				.contentType("application/json")
				.content("{\"items\":[{\"productOrServiceId\":\"" + productId + "\",\"quantity\":10}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderId").value(order.getId().value().toString()))
				.andExpect(jsonPath("$.total").value(true))
				.andExpect(jsonPath("$.returnNfeRef.documentId").value(returnNfeRef.documentId().toString()));

		verify(registerStockEntryPort).registerEntry(any());
		verify(adjustReceivableForReturnPort).adjust(any());
	}

	@Test
	void returningAnOrderThatIsNotInvoicedIsRejectedWithBadRequest() throws Exception {
		UUID productId = seedProduct();
		SalesOrder order = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), List.of(new SalesOrderItem(productId, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				SalesOrderStatus.APPROVED, UUID.randomUUID(), null);
		salesOrderRepositoryPort.save(order);

		mockMvc.perform(post("/api/sales/orders/" + order.getId().value() + "/return")
				.contentType("application/json")
				.content("{\"items\":[{\"productOrServiceId\":\"" + productId + "\",\"quantity\":1}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void anOrderThatDoesNotExistIsRejectedWith404() throws Exception {
		mockMvc.perform(post("/api/sales/orders/" + UUID.randomUUID() + "/return")
				.contentType("application/json")
				.content("{\"items\":[{\"productOrServiceId\":\"" + UUID.randomUUID() + "\",\"quantity\":1}]}"))
				.andExpect(status().isNotFound());
	}

	private SalesOrder persistInvoicedOrder(UUID productId, BigDecimal quantity, BigDecimal unitPrice,
			FiscalDocumentRef fiscalDocumentRef) {
		SalesOrder order = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), List.of(new SalesOrderItem(productId, quantity, unitPrice, BigDecimal.ZERO)),
				SalesOrderStatus.INVOICED, UUID.randomUUID(), null);
		SalesOrder saved = salesOrderRepositoryPort.save(order);
		SalesInvoice invoice = SalesInvoice.issue(SalesInvoiceId.of(UUID.randomUUID()), saved.getId(),
				List.of(fiscalDocumentRef));
		salesInvoiceRepositoryPort.save(invoice);
		return saved;
	}

	private UUID seedProduct() {
		ProductDomain product = ProductDomain.builder().type(ProductType.SIMPLE).status(ProductStatus.ACTIVE)
				.internalCode("SKU-" + UUID.randomUUID()).averageCost(new BigDecimal("3.00")).build();
		product.setId(UUID.randomUUID());
		return productRepositoryPort.save(product).getId();
	}
}
