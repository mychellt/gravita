package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesInvoice;
import br.gravita.core.domain.sales.SalesInvoiceId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderNotFoundException;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.sales.SalesReturn;
import br.gravita.core.domain.sales.SalesReturnId;
import br.gravita.core.domain.sales.SalesReturnItem;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.ReturnSalesOrderCommand;
import br.gravita.core.ports.inbound.sales.ReturnSalesOrderCommand.Item;
import br.gravita.core.ports.inbound.sales.SalesReturnView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesInvoiceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesReturnRepositoryPort;
import br.gravita.core.ports.outbound.sales.AdjustReceivableForReturnPort;
import br.gravita.core.ports.outbound.sales.AdjustReceivableForReturnPort.AdjustReceivableForReturnCommand;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort.IssueReturnFiscalDocumentCommand;
import br.gravita.core.ports.outbound.sales.RegisterStockEntryPort;
import br.gravita.core.ports.outbound.sales.RegisterStockEntryPort.RegisterStockEntryCommand;
import br.gravita.core.usercases.sales.ReturnSalesOrderService;
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
class ReturnSalesOrderServiceTest {

	@Mock
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Mock
	private SalesInvoiceRepositoryPort salesInvoiceRepositoryPort;

	@Mock
	private SalesReturnRepositoryPort salesReturnRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	@Mock
	private IssueFiscalDocumentPort issueFiscalDocumentPort;

	@Mock
	private RegisterStockEntryPort registerStockEntryPort;

	@Mock
	private AdjustReceivableForReturnPort adjustReceivableForReturnPort;

	@InjectMocks
	private ReturnSalesOrderService service;

	private final UUID productId = UUID.randomUUID();
	private final FiscalDocumentRef originalNfeRef = new FiscalDocumentRef(FiscalDocumentType.NFE, UUID.randomUUID());

	@Test
	@DisplayName("Returning every invoiced item in full reverts stock, adjusts the receivable and issues the return NF-e")
	void returningEveryInvoicedItemInFullRevertsStockAdjustsTheReceivableAndIssuesTheReturnNfe() {
		SalesOrder order = invoicedOrder(BigDecimal.TEN, "5.00");
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(salesReturnRepositoryPort.findByOrderId(order.getId())).thenReturn(List.of());
		when(salesInvoiceRepositoryPort.findByOrderId(order.getId()))
				.thenReturn(Optional.of(invoiceFor(order, originalNfeRef)));
		when(productRepositoryPort.get(productId))
				.thenReturn(Optional.of(ProductDomain.builder().internalCode("SKU-1").averageCost(new BigDecimal("3.00")).build()));
		FiscalDocumentRef returnNfeRef = new FiscalDocumentRef(FiscalDocumentType.NFE, UUID.randomUUID());
		when(issueFiscalDocumentPort.issueForReturn(any())).thenReturn(returnNfeRef);
		when(salesReturnRepositoryPort.save(any(SalesReturn.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SalesReturnView view = service
				.execute(new ReturnSalesOrderCommand(order.getId().value(), List.of(new Item(productId, BigDecimal.TEN))));

		assertThat(view.orderId()).isEqualTo(order.getId().value());
		assertThat(view.returnNfeRef()).isEqualTo(returnNfeRef);
		assertThat(view.total()).isTrue();

		ArgumentCaptor<RegisterStockEntryCommand> stockEntry = ArgumentCaptor.forClass(RegisterStockEntryCommand.class);
		verify(registerStockEntryPort, times(1)).registerEntry(stockEntry.capture());
		assertThat(stockEntry.getValue().productOrServiceId()).isEqualTo(productId);
		assertThat(stockEntry.getValue().quantity()).isEqualByComparingTo(BigDecimal.TEN);
		assertThat(stockEntry.getValue().unitCost()).isEqualByComparingTo("3.00");

		ArgumentCaptor<AdjustReceivableForReturnCommand> receivable = ArgumentCaptor
				.forClass(AdjustReceivableForReturnCommand.class);
		verify(adjustReceivableForReturnPort).adjust(receivable.capture());
		assertThat(receivable.getValue().customerId()).isEqualTo(order.getCustomerId());
		assertThat(receivable.getValue().amount()).isEqualByComparingTo("50.00");

		ArgumentCaptor<IssueReturnFiscalDocumentCommand> nfeCommand = ArgumentCaptor
				.forClass(IssueReturnFiscalDocumentCommand.class);
		verify(issueFiscalDocumentPort).issueForReturn(nfeCommand.capture());
		assertThat(nfeCommand.getValue().originalDocument()).isEqualTo(originalNfeRef);
		assertThat(nfeCommand.getValue().items()).hasSize(1);
	}

	@Test
	@DisplayName("Returning fewer units than were invoiced is recorded as a partial return")
	void returningFewerThanTheInvoicedQuantityIsRecordedAsPartial() {
		SalesOrder order = invoicedOrder(BigDecimal.TEN, "5.00");
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(salesReturnRepositoryPort.findByOrderId(order.getId())).thenReturn(List.of());
		when(salesInvoiceRepositoryPort.findByOrderId(order.getId()))
				.thenReturn(Optional.of(invoiceFor(order, originalNfeRef)));
		when(productRepositoryPort.get(productId))
				.thenReturn(Optional.of(ProductDomain.builder().internalCode("SKU-1").build()));
		when(salesReturnRepositoryPort.save(any(SalesReturn.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SalesReturnView view = service.execute(new ReturnSalesOrderCommand(order.getId().value(),
				List.of(new Item(productId, new BigDecimal("4")))));

		assertThat(view.total()).isFalse();

		ArgumentCaptor<AdjustReceivableForReturnCommand> receivable = ArgumentCaptor
				.forClass(AdjustReceivableForReturnCommand.class);
		verify(adjustReceivableForReturnPort).adjust(receivable.capture());
		assertThat(receivable.getValue().amount()).isEqualByComparingTo("20.00");
	}

	@Test
	@DisplayName("A second partial return that completes the invoiced quantity is recorded as a total return")
	void aSecondPartialReturnThatCompletesTheInvoicedQuantityIsRecordedAsTotal() {
		SalesOrder order = invoicedOrder(BigDecimal.TEN, "5.00");
		SalesReturn firstReturn = SalesReturn.forOrder(SalesReturnId.of(UUID.randomUUID()), order,
				List.of(new SalesReturnItem(productId, new BigDecimal("6"))), List.of());
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(salesReturnRepositoryPort.findByOrderId(order.getId())).thenReturn(List.of(firstReturn));
		when(salesInvoiceRepositoryPort.findByOrderId(order.getId()))
				.thenReturn(Optional.of(invoiceFor(order, originalNfeRef)));
		when(productRepositoryPort.get(productId))
				.thenReturn(Optional.of(ProductDomain.builder().internalCode("SKU-1").build()));
		when(salesReturnRepositoryPort.save(any(SalesReturn.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SalesReturnView view = service.execute(new ReturnSalesOrderCommand(order.getId().value(),
				List.of(new Item(productId, new BigDecimal("4")))));

		assertThat(view.total()).isTrue();
	}

	@Test
	@DisplayName("Rejects a return against an order that is not invoiced")
	void rejectsAReturnAgainstAnOrderThatIsNotInvoiced() {
		SalesOrder order = orderWithStatus(SalesOrderStatus.APPROVED, BigDecimal.TEN, "5.00");
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.execute(
				new ReturnSalesOrderCommand(order.getId().value(), List.of(new Item(productId, BigDecimal.TEN)))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("Only INVOICED orders can be returned");

		verify(registerStockEntryPort, never()).registerEntry(any());
		verify(adjustReceivableForReturnPort, never()).adjust(any());
		verify(issueFiscalDocumentPort, never()).issueForReturn(any());
		verify(salesReturnRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a return quantity above what was invoiced, with no side effects")
	void rejectsAReturnQuantityExceedingWhatWasOriginallyInvoicedWithoutAnySideEffect() {
		SalesOrder order = invoicedOrder(BigDecimal.TEN, "5.00");
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(salesReturnRepositoryPort.findByOrderId(order.getId())).thenReturn(List.of());

		assertThatThrownBy(() -> service.execute(new ReturnSalesOrderCommand(order.getId().value(),
				List.of(new Item(productId, new BigDecimal("11"))))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("exceeds the quantity originally invoiced");

		verify(registerStockEntryPort, never()).registerEntry(any());
		verify(adjustReceivableForReturnPort, never()).adjust(any());
		verify(salesReturnRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects returning more than what remains after a prior return")
	void rejectsReturningMoreThanWhatRemainsAfterAPriorReturn() {
		SalesOrder order = invoicedOrder(BigDecimal.TEN, "5.00");
		SalesReturn firstReturn = SalesReturn.forOrder(SalesReturnId.of(UUID.randomUUID()), order,
				List.of(new SalesReturnItem(productId, new BigDecimal("6"))), List.of());
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(salesReturnRepositoryPort.findByOrderId(order.getId())).thenReturn(List.of(firstReturn));

		assertThatThrownBy(() -> service.execute(new ReturnSalesOrderCommand(order.getId().value(),
				List.of(new Item(productId, new BigDecimal("5"))))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("exceeds the quantity originally invoiced");

		verify(salesReturnRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a return for an order that does not exist")
	void rejectsAReturnForAnOrderThatDoesNotExist() {
		UUID orderId = UUID.randomUUID();
		when(salesOrderRepositoryPort.findById(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service
				.execute(new ReturnSalesOrderCommand(orderId, List.of(new Item(productId, BigDecimal.ONE)))))
				.isInstanceOf(SalesOrderNotFoundException.class);

		verify(salesReturnRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a return when the product or service of an item cannot be found")
	void rejectsAReturnWhenAnItemsProductOrServiceCannotBeFound() {
		SalesOrder order = invoicedOrder(BigDecimal.TEN, "5.00");
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(salesReturnRepositoryPort.findByOrderId(order.getId())).thenReturn(List.of());
		when(salesInvoiceRepositoryPort.findByOrderId(order.getId()))
				.thenReturn(Optional.of(invoiceFor(order, originalNfeRef)));
		when(productRepositoryPort.get(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new ReturnSalesOrderCommand(order.getId().value(), List.of(new Item(productId, BigDecimal.TEN)))))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(salesReturnRepositoryPort, never()).save(any());
	}

	private SalesOrder invoicedOrder(BigDecimal quantity, String unitPrice) {
		return orderWithStatus(SalesOrderStatus.INVOICED, quantity, unitPrice);
	}

	private SalesOrder orderWithStatus(SalesOrderStatus status, BigDecimal quantity, String unitPrice) {
		return SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()), UUID.randomUUID(),
				UUID.randomUUID(),
				List.of(new SalesOrderItem(productId, quantity, new BigDecimal(unitPrice), BigDecimal.ZERO)), status,
				UUID.randomUUID(), null);
	}

	private SalesInvoice invoiceFor(SalesOrder order, FiscalDocumentRef fiscalDocumentRef) {
		return SalesInvoice.issue(SalesInvoiceId.of(UUID.randomUUID()), order.getId(), List.of(fiscalDocumentRef));
	}
}
