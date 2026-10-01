package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.Commission;
import br.gravita.core.domain.sales.CommissionRate;
import br.gravita.core.domain.sales.CommissionRateNotFoundException;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.ports.inbound.sales.CalculateCommissionQuery;
import br.gravita.core.ports.inbound.sales.CommissionView;
import br.gravita.core.ports.outbound.persistence.sales.CommissionRateRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.CommissionRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.usercases.sales.CalculateCommissionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
class CalculateCommissionServiceTest {

	@Mock
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Mock
	private CommissionRateRepositoryPort commissionRateRepositoryPort;

	@Mock
	private CommissionRepositoryPort commissionRepositoryPort;

	@InjectMocks
	private CalculateCommissionService service;

	private static final YearMonth PERIOD = YearMonth.of(2026, 9);

	@Test
	@DisplayName("Calculates and persists a commission per invoiced order item using the configured rate")
	void calculatesAndPersistsACommissionPerInvoicedOrderItemUsingTheConfiguredRate() {
		UUID salespersonId = UUID.randomUUID();
		UUID productA = UUID.randomUUID();
		UUID productB = UUID.randomUUID();
		SalesOrder order = invoicedOrder(salespersonId,
				List.of(item(productA, new BigDecimal("2"), new BigDecimal("10.00"), BigDecimal.ZERO),
						item(productB, BigDecimal.ONE, new BigDecimal("50.00"), BigDecimal.ZERO)));
		when(salesOrderRepositoryPort.findInvoicedByPeriod(PERIOD.atDay(1), PERIOD.atEndOfMonth()))
				.thenReturn(List.of(order));
		when(commissionRateRepositoryPort.findBySalespersonAndProduct(salespersonId, productA))
				.thenReturn(Optional.of(new CommissionRate(salespersonId, productA, new BigDecimal("0.10"))));
		when(commissionRateRepositoryPort.findBySalespersonAndProduct(salespersonId, productB))
				.thenReturn(Optional.of(new CommissionRate(salespersonId, productB, new BigDecimal("0.05"))));
		when(commissionRepositoryPort.save(any(Commission.class))).thenAnswer(invocation -> invocation.getArgument(0));

		List<CommissionView> views = service.execute(new CalculateCommissionQuery(null, PERIOD));

		assertThat(views).hasSize(2);
		assertThat(views.get(0).salespersonId()).isEqualTo(salespersonId);
		assertThat(views.get(0).productId()).isEqualTo(productA);
		assertThat(views.get(0).rate()).isEqualByComparingTo("0.10");
		assertThat(views.get(0).amount()).isEqualByComparingTo("2.0000");
		assertThat(views.get(1).productId()).isEqualTo(productB);
		assertThat(views.get(1).amount()).isEqualByComparingTo("2.5000");

		ArgumentCaptor<Commission> saved = ArgumentCaptor.forClass(Commission.class);
		verify(commissionRepositoryPort, org.mockito.Mockito.times(2)).save(saved.capture());
		assertThat(saved.getAllValues()).allSatisfy(c -> assertThat(c.orderId()).isEqualTo(order.getId()));

		verify(salesOrderRepositoryPort, never()).findInvoicedByPeriodAndSalesperson(any(), any(), any());
	}

	@Test
	@DisplayName("Considers only the given salesperson's orders when a salesperson is provided")
	void filtersByTheGivenSalespersonWhenProvided() {
		UUID salespersonId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		SalesOrder order = invoicedOrder(salespersonId,
				List.of(item(productId, BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO)));
		when(salesOrderRepositoryPort.findInvoicedByPeriodAndSalesperson(PERIOD.atDay(1), PERIOD.atEndOfMonth(),
				salespersonId)).thenReturn(List.of(order));
		when(commissionRateRepositoryPort.findBySalespersonAndProduct(salespersonId, productId))
				.thenReturn(Optional.of(new CommissionRate(salespersonId, productId, new BigDecimal("0.20"))));
		when(commissionRepositoryPort.save(any(Commission.class))).thenAnswer(invocation -> invocation.getArgument(0));

		List<CommissionView> views = service.execute(new CalculateCommissionQuery(salespersonId, PERIOD));

		assertThat(views).hasSize(1);
		assertThat(views.get(0).amount()).isEqualByComparingTo("20.0000");
		verify(salesOrderRepositoryPort, never()).findInvoicedByPeriod(any(), any());
	}

	@Test
	@DisplayName("Rejects the calculation when no commission rate is configured for the salesperson and product pair")
	void rejectsCalculatingWhenNoRateIsConfiguredForThePair() {
		UUID salespersonId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		SalesOrder order = invoicedOrder(salespersonId,
				List.of(item(productId, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)));
		when(salesOrderRepositoryPort.findInvoicedByPeriod(PERIOD.atDay(1), PERIOD.atEndOfMonth()))
				.thenReturn(List.of(order));
		when(commissionRateRepositoryPort.findBySalespersonAndProduct(salespersonId, productId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new CalculateCommissionQuery(null, PERIOD)))
				.isInstanceOf(CommissionRateNotFoundException.class);

		verify(commissionRepositoryPort, never()).save(any());
	}

	private static SalesOrder invoicedOrder(UUID salespersonId, List<SalesOrderItem> items) {
		SalesOrder order = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), salespersonId, items, SalesOrderStatus.APPROVED, UUID.randomUUID(), null);
		return SalesOrder.of(order.getId(), order.getOriginQuoteId(), order.getCustomerId(), salespersonId, items,
				SalesOrderStatus.INVOICED, order.getApprovedBy(), order.getAlcadaId(), null, LocalDate.now());
	}

	private static SalesOrderItem item(UUID productOrServiceId, BigDecimal quantity, BigDecimal unitPrice,
			BigDecimal discount) {
		return new SalesOrderItem(productOrServiceId, quantity, unitPrice, discount);
	}
}
