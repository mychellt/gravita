package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalespersonTarget;
import br.gravita.core.ports.inbound.sales.GetTargetProgressQuery;
import br.gravita.core.ports.inbound.sales.GetTargetProgressUseCase;
import br.gravita.core.ports.inbound.sales.TargetProgressView;
import br.gravita.core.ports.inbound.sales.TargetProgressView.PercentComplete;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalespersonTargetRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetTargetProgressService implements GetTargetProgressUseCase {

	private final SalespersonTargetRepositoryPort salespersonTargetRepositoryPort;
	private final SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Override
	public TargetProgressView execute(GetTargetProgressQuery query) {
		List<SalesOrder> invoicedOrders = salesOrderRepositoryPort.findInvoicedByPeriodAndSalesperson(
				query.month().atDay(1), query.month().atEndOfMonth(), query.salesperson());

		BigDecimal valueAchieved = invoicedOrders.stream().map(SalesOrder::totalValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		long orderCountAchieved = invoicedOrders.size();

		return salespersonTargetRepositoryPort.findBySalespersonAndMonth(query.salesperson(), query.month())
				.map(target -> withTarget(valueAchieved, orderCountAchieved, target))
				.orElseGet(() -> noTargetConfigured(valueAchieved, orderCountAchieved));
	}

	private TargetProgressView withTarget(BigDecimal valueAchieved, long orderCountAchieved,
			SalespersonTarget target) {
		PercentComplete percentComplete = new PercentComplete(percentOf(valueAchieved, target.valueTarget()),
				percentOf(BigDecimal.valueOf(orderCountAchieved), BigDecimal.valueOf(target.orderCountTarget())));
		return new TargetProgressView(valueAchieved, orderCountAchieved, target.valueTarget(),
				target.orderCountTarget(), percentComplete, true);
	}

	private TargetProgressView noTargetConfigured(BigDecimal valueAchieved, long orderCountAchieved) {
		return new TargetProgressView(valueAchieved, orderCountAchieved, BigDecimal.ZERO, 0L, null, false);
	}

	private BigDecimal percentOf(BigDecimal achieved, BigDecimal target) {
		if (target.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return achieved.multiply(BigDecimal.valueOf(100)).divide(target, 4, RoundingMode.HALF_UP);
	}
}
