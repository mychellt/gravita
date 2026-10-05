package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.Commission;
import br.gravita.core.domain.sales.CommissionId;
import br.gravita.core.domain.sales.CommissionRate;
import br.gravita.core.domain.sales.CommissionRateNotFoundException;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.ports.inbound.sales.CalculateCommissionQuery;
import br.gravita.core.ports.inbound.sales.CalculateCommissionUseCase;
import br.gravita.core.ports.inbound.sales.CommissionView;
import br.gravita.core.ports.outbound.persistence.sales.CommissionRateRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.CommissionRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CalculateCommissionService implements CalculateCommissionUseCase {

	private final SalesOrderRepositoryPort salesOrderRepositoryPort;
	private final CommissionRateRepositoryPort commissionRateRepositoryPort;
	private final CommissionRepositoryPort commissionRepositoryPort;

	@Override
	public List<CommissionView> execute(final CalculateCommissionQuery query) {
		final var periodStart = query.period().atDay(1);
		final var periodEnd = query.period().atEndOfMonth();

		final List<SalesOrder> orders = query.salesperson() == null
				? salesOrderRepositoryPort.findInvoicedByPeriod(periodStart, periodEnd)
				: salesOrderRepositoryPort.findInvoicedByPeriodAndSalesperson(periodStart, periodEnd,
						query.salesperson());

		final List<CommissionView> views = new ArrayList<>();
		for (final SalesOrder order : orders) {
			for (final SalesOrderItem item : order.getItems()) {
				final Commission commission = calculateForItem(order, item);
				final CommissionView view = CommissionView.from(commissionRepositoryPort.save(commission));
				views.add(view);
			}
		}
		return views;
	}

	private Commission calculateForItem(final SalesOrder order, final SalesOrderItem item) {
		final UUID salespersonId = order.getSalespersonId();
		final UUID productId = item.productOrServiceId();
		final CommissionRate commissionRate = commissionRateRepositoryPort
				.findBySalespersonAndProduct(salespersonId, productId)
				.orElseThrow(() -> new CommissionRateNotFoundException(salespersonId, productId));

		return Commission.calculate(CommissionId.of(UUID.randomUUID()), salespersonId, productId, order.getId(),
				commissionRate.rate(), item.lineTotal());
	}
}
