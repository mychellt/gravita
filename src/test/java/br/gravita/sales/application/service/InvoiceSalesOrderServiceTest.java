package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesInvoice;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderNotFoundException;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.InvoiceSalesOrderCommand;
import br.gravita.core.ports.inbound.sales.SalesInvoiceView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesInvoiceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.sales.GenerateAccountsReceivablePort;
import br.gravita.core.ports.outbound.sales.GenerateAccountsReceivablePort.GenerateAccountsReceivableCommand;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort.IssueFiscalDocumentCommand;
import br.gravita.core.usercases.sales.InvoiceSalesOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceSalesOrderServiceTest {

	@Mock
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Mock
	private SalesInvoiceRepositoryPort salesInvoiceRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	@Mock
	private IssueFiscalDocumentPort issueFiscalDocumentPort;

	@Mock
	private GenerateAccountsReceivablePort generateAccountsReceivablePort;

	@InjectMocks
	private InvoiceSalesOrderService service;

	@Test
	@DisplayName("Invoices an approved product-only order through NF-e and generates a receivable for the total")
	void invoicesAnApprovedOrderWithOnlyProductItemsThroughNfeAndGeneratesAReceivableForTheTotal() {
		UUID productId = UUID.randomUUID();
		SalesOrder order = orderWithStatus(SalesOrderStatus.APPROVED,
				List.of(item(productId, new BigDecimal("2"), new BigDecimal("10.00"), BigDecimal.ZERO)));
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(product(ProductType.SIMPLE)));
		FiscalDocumentRef nfeRef = new FiscalDocumentRef(FiscalDocumentType.NFE, UUID.randomUUID());
		when(issueFiscalDocumentPort.issueForProducts(any())).thenReturn(nfeRef);
		when(salesOrderRepositoryPort.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(salesInvoiceRepositoryPort.save(any(SalesInvoice.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SalesInvoiceView view = service.execute(new InvoiceSalesOrderCommand(order.getId().value()));

		assertThat(view.orderId()).isEqualTo(order.getId().value());
		assertThat(view.fiscalDocuments()).containsExactly(nfeRef);

		ArgumentCaptor<SalesOrder> savedOrder = ArgumentCaptor.forClass(SalesOrder.class);
		verify(salesOrderRepositoryPort).save(savedOrder.capture());
		assertThat(savedOrder.getValue().getStatus()).isEqualTo(SalesOrderStatus.INVOICED);

		ArgumentCaptor<IssueFiscalDocumentCommand> issued = ArgumentCaptor.forClass(IssueFiscalDocumentCommand.class);
		verify(issueFiscalDocumentPort).issueForProducts(issued.capture());
		assertThat(issued.getValue().orderId()).isEqualTo(order.getId().value());
		assertThat(issued.getValue().customerId()).isEqualTo(order.getCustomerId());
		assertThat(issued.getValue().items()).hasSize(1);
		verify(issueFiscalDocumentPort, never()).issueForServices(any());

		ArgumentCaptor<GenerateAccountsReceivableCommand> receivable = ArgumentCaptor
				.forClass(GenerateAccountsReceivableCommand.class);
		verify(generateAccountsReceivablePort).generate(receivable.capture());
		assertThat(receivable.getValue().originDocument()).isEqualTo(nfeRef);
		assertThat(receivable.getValue().amount()).isEqualByComparingTo("20.00");
	}

	@Test
	@DisplayName("Rejects invoicing an order that does not exist")
	void rejectsInvoicingAnOrderThatDoesNotExist() {
		UUID orderId = UUID.randomUUID();
		when(salesOrderRepositoryPort.findById(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new InvoiceSalesOrderCommand(orderId)))
				.isInstanceOf(SalesOrderNotFoundException.class);

		verify(issueFiscalDocumentPort, never()).issueForProducts(any());
		verify(salesOrderRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects invoicing a draft order and issues no fiscal document")
	void rejectsInvoicingADraftOrderWithoutIssuingAnyFiscalDocument() {
		SalesOrder order = orderWithStatus(SalesOrderStatus.DRAFT,
				List.of(item(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)));
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.execute(new InvoiceSalesOrderCommand(order.getId().value())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Only APPROVED or IN_SEPARATION orders can be invoiced");

		verify(issueFiscalDocumentPort, never()).issueForProducts(any());
		verify(issueFiscalDocumentPort, never()).issueForServices(any());
		verify(salesOrderRepositoryPort, never()).save(any());
		verify(salesInvoiceRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects invoicing when the product or service of an item cannot be found")
	void rejectsInvoicingWhenAnItemsProductOrServiceCannotBeFound() {
		UUID productId = UUID.randomUUID();
		SalesOrder order = orderWithStatus(SalesOrderStatus.APPROVED,
				List.of(item(productId, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)));
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new InvoiceSalesOrderCommand(order.getId().value())))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(salesOrderRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Routes service line items to NFS-e issuance and links the resulting document")
	void routesServiceLineItemsToNfseIssuanceAndLinksTheResultingDocument() {
		UUID serviceId = UUID.randomUUID();
		SalesOrder order = orderWithStatus(SalesOrderStatus.IN_SEPARATION,
				List.of(item(serviceId, BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO)));
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(productRepositoryPort.get(serviceId)).thenReturn(Optional.of(product(ProductType.SERVICE)));
		FiscalDocumentRef nfseRef = new FiscalDocumentRef(FiscalDocumentType.NFSE, UUID.randomUUID());
		when(issueFiscalDocumentPort.issueForServices(any())).thenReturn(nfseRef);
		when(salesOrderRepositoryPort.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(salesInvoiceRepositoryPort.save(any(SalesInvoice.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SalesInvoiceView view = service.execute(new InvoiceSalesOrderCommand(order.getId().value()));

		assertThat(view.fiscalDocuments()).containsExactly(nfseRef);
		verify(issueFiscalDocumentPort, never()).issueForProducts(any());
	}

	@Test
	@DisplayName("Propagates an NFS-e unavailable failure without transitioning the order")
	void propagatesTheNfseNotAvailableFailureWithoutTransitioningTheOrder() {
		UUID serviceId = UUID.randomUUID();
		SalesOrder order = orderWithStatus(SalesOrderStatus.APPROVED,
				List.of(item(serviceId, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)));
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(productRepositoryPort.get(serviceId)).thenReturn(Optional.of(product(ProductType.SERVICE)));
		when(issueFiscalDocumentPort.issueForServices(any()))
				.thenThrow(new BusinessRuleException("NFSe issuance is not yet available"));

		assertThatThrownBy(() -> service.execute(new InvoiceSalesOrderCommand(order.getId().value())))
				.isInstanceOf(BusinessRuleException.class);

		verify(salesOrderRepositoryPort, never()).save(any());
		verify(salesInvoiceRepositoryPort, never()).save(any());
		verify(generateAccountsReceivablePort, never()).generate(any());
	}

	private static SalesOrder orderWithStatus(SalesOrderStatus status, List<SalesOrderItem> items) {
		return SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(), items, status, status == SalesOrderStatus.DRAFT ? null : UUID.randomUUID(), null);
	}

	private static SalesOrderItem item(UUID productOrServiceId, BigDecimal quantity, BigDecimal unitPrice,
			BigDecimal discount) {
		return new SalesOrderItem(productOrServiceId, quantity, unitPrice, discount);
	}

	private static ProductDomain product(ProductType type) {
		ProductDomain product = ProductDomain.builder().type(type).internalCode("SKU-1").build();
		return product;
	}
}
