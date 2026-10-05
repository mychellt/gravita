package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.sales.SalespersonTarget;
import br.gravita.core.ports.inbound.sales.GetTargetProgressQuery;
import br.gravita.core.ports.inbound.sales.TargetProgressView;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalespersonTargetRepositoryPort;
import br.gravita.core.usercases.sales.GetTargetProgressService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetTargetProgressServiceTest {

	@Mock
	private SalespersonTargetRepositoryPort salespersonTargetRepositoryPort;

	@Mock
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@InjectMocks
	private GetTargetProgressService service;

	private static final YearMonth MONTH = YearMonth.of(2026, 9);

	@Test
	@DisplayName("Reports the achieved value and order count against the configured target")
	void reportsAchievedValueAndOrderCountAgainstTheConfiguredTarget() {
		final UUID salespersonId = UUID.randomUUID();
		final SalesOrder orderA = invoicedOrder(salespersonId, new BigDecimal("300.00"));
		final SalesOrder orderB = invoicedOrder(salespersonId, new BigDecimal("200.00"));
		when(salesOrderRepositoryPort.findInvoicedByPeriodAndSalesperson(MONTH.atDay(1), MONTH.atEndOfMonth(),
				salespersonId)).thenReturn(List.of(orderA, orderB));
		when(salespersonTargetRepositoryPort.findBySalespersonAndMonth(salespersonId, MONTH))
				.thenReturn(Optional.of(new SalespersonTarget(salespersonId, MONTH, new BigDecimal("1000.00"), 4)));

		final TargetProgressView view = service.execute(new GetTargetProgressQuery(salespersonId, MONTH));

		assertThat(view.targetConfigured()).isTrue();
		assertThat(view.valueAchieved()).isEqualByComparingTo("500.00");
		assertThat(view.orderCountAchieved()).isEqualTo(2);
		assertThat(view.valueTarget()).isEqualByComparingTo("1000.00");
		assertThat(view.orderCountTarget()).isEqualTo(4);
		assertThat(view.percentComplete().value()).isEqualByComparingTo("50.0000");
		assertThat(view.percentComplete().orderCount()).isEqualByComparingTo("50.0000");
	}

	@Test
	@DisplayName("Returns a \"no target configured\" result instead of dividing by zero")
	void returnsANoTargetConfiguredResultInsteadOfDividingByZero() {
		final UUID salespersonId = UUID.randomUUID();
		when(salesOrderRepositoryPort.findInvoicedByPeriodAndSalesperson(MONTH.atDay(1), MONTH.atEndOfMonth(),
				salespersonId)).thenReturn(List.of(invoicedOrder(salespersonId, new BigDecimal("100.00"))));
		when(salespersonTargetRepositoryPort.findBySalespersonAndMonth(salespersonId, MONTH))
				.thenReturn(Optional.empty());

		final TargetProgressView view = service.execute(new GetTargetProgressQuery(salespersonId, MONTH));

		assertThat(view.targetConfigured()).isFalse();
		assertThat(view.valueAchieved()).isEqualByComparingTo("100.00");
		assertThat(view.orderCountAchieved()).isEqualTo(1);
		assertThat(view.percentComplete()).isNull();
	}

	@Test
	@DisplayName("Only orders invoiced in the requested month count towards the progress")
	void onlyOrdersInvoicedInTheRequestedMonthAreConsidered() {
		final UUID salespersonId = UUID.randomUUID();
		when(salesOrderRepositoryPort.findInvoicedByPeriodAndSalesperson(MONTH.atDay(1), MONTH.atEndOfMonth(),
				salespersonId)).thenReturn(List.of());
		when(salespersonTargetRepositoryPort.findBySalespersonAndMonth(any(), any())).thenReturn(Optional.empty());

		final TargetProgressView view = service.execute(new GetTargetProgressQuery(salespersonId, MONTH));

		assertThat(view.valueAchieved()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(view.orderCountAchieved()).isZero();
	}

	private static SalesOrder invoicedOrder(final UUID salespersonId, final BigDecimal itemValue) {
		return SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()), UUID.randomUUID(),
				salespersonId, List.of(new SalesOrderItem(UUID.randomUUID(), BigDecimal.ONE, itemValue, BigDecimal.ZERO)),
				SalesOrderStatus.INVOICED, UUID.randomUUID(), null, null, LocalDate.now());
	}
}
