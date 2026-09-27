package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderNotFoundException;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.CancelSalesOrderCommand;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.sales.ReleaseStockReservationPort;
import br.gravita.core.usercases.sales.CancelSalesOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelSalesOrderServiceTest {

	@Mock
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Mock
	private ReleaseStockReservationPort releaseStockReservationPort;

	@InjectMocks
	private CancelSalesOrderService service;

	@Test
	void cancelsADraftOrderWithoutReleasingAnyStockReservation() {
		SalesOrder order = SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()),
				QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), List.of(item()));
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(salesOrderRepositoryPort.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new CancelSalesOrderCommand(order.getId().value(), "Customer requested cancellation"));

		ArgumentCaptor<SalesOrder> saved = ArgumentCaptor.forClass(SalesOrder.class);
		verify(salesOrderRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);
		assertThat(saved.getValue().getCancelReason()).isEqualTo("Customer requested cancellation");
		verify(releaseStockReservationPort, never()).releaseByOrderRef(any());
	}

	@Test
	void cancellingAnApprovedOrderReleasesTheOrdersStockReservations() {
		SalesOrder order = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), List.of(item()), SalesOrderStatus.APPROVED, UUID.randomUUID(), null);
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(salesOrderRepositoryPort.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new CancelSalesOrderCommand(order.getId().value(), "Out of stock"));

		verify(releaseStockReservationPort).releaseByOrderRef(order.getId().value());

		ArgumentCaptor<SalesOrder> saved = ArgumentCaptor.forClass(SalesOrder.class);
		verify(salesOrderRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(SalesOrderStatus.CANCELLED);
	}

	@Test
	void cancellingAnInSeparationOrderReleasesItsStockReservations() {
		SalesOrder order = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), List.of(item()), SalesOrderStatus.IN_SEPARATION, UUID.randomUUID(), null);
		when(salesOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(salesOrderRepositoryPort.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new CancelSalesOrderCommand(order.getId().value(), "Customer changed mind"));

		verify(releaseStockReservationPort).releaseByOrderRef(order.getId().value());
	}

	@Test
	void rejectsCancellingAnInvoicedOrderWithoutReleasingStockOrSaving() {
		SalesOrder invoiced = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), List.of(item()), SalesOrderStatus.INVOICED, UUID.randomUUID(), null);
		when(salesOrderRepositoryPort.findById(invoiced.getId())).thenReturn(Optional.of(invoiced));

		assertThatThrownBy(() -> service.execute(
				new CancelSalesOrderCommand(invoiced.getId().value(), "Customer changed mind")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("return flow");

		verify(releaseStockReservationPort, never()).releaseByOrderRef(any());
		verify(salesOrderRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsCancellingAnOrderThatDoesNotExist() {
		UUID orderId = UUID.randomUUID();
		when(salesOrderRepositoryPort.findById(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new CancelSalesOrderCommand(orderId, "Any reason")))
				.isInstanceOf(SalesOrderNotFoundException.class);

		verify(releaseStockReservationPort, never()).releaseByOrderRef(any());
		verify(salesOrderRepositoryPort, never()).save(any());
	}

	private static SalesOrderItem item() {
		return new SalesOrderItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO);
	}
}
