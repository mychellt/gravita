package br.gravita.adapters.outbound.reporting;

import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.Commission;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalespersonTarget;
import br.gravita.core.ports.outbound.persistence.sales.CommissionRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalespersonTargetRepositoryPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sales orders carry no company, so {@code companyId} cannot narrow these reads yet and is accepted only to keep
 * the port stable for when they do.
 */
@PersistenceAdapter
class SalesReadModelAdapter implements SalesReadModelPort {

	private final SalesOrderRepositoryPort salesOrderRepositoryPort;
	private final SalespersonTargetRepositoryPort salespersonTargetRepositoryPort;
	private final CommissionRepositoryPort commissionRepositoryPort;

	SalesReadModelAdapter(SalesOrderRepositoryPort salesOrderRepositoryPort,
			SalespersonTargetRepositoryPort salespersonTargetRepositoryPort,
			CommissionRepositoryPort commissionRepositoryPort) {
		this.salesOrderRepositoryPort = salesOrderRepositoryPort;
		this.salespersonTargetRepositoryPort = salespersonTargetRepositoryPort;
		this.commissionRepositoryPort = commissionRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<LocalDate, BigDecimal> dailyRevenue(LocalDate from, LocalDate to, UUID companyId) {
		Map<LocalDate, BigDecimal> revenue = new HashMap<>();
		for (SalesOrder order : salesOrderRepositoryPort.findInvoicedByPeriod(from, to)) {
			revenue.merge(order.getInvoicedAt(), order.totalValue(), BigDecimal::add);
		}
		return revenue;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductSales> productSales(LocalDate from, LocalDate to, UUID companyId) {
		Map<UUID, BigDecimal> quantities = new HashMap<>();
		Map<UUID, BigDecimal> values = new HashMap<>();
		for (SalesOrder order : salesOrderRepositoryPort.findInvoicedByPeriod(from, to)) {
			for (SalesOrderItem item : order.getItems()) {
				quantities.merge(item.productOrServiceId(), item.quantity(), BigDecimal::add);
				values.merge(item.productOrServiceId(), item.lineTotal(), BigDecimal::add);
			}
		}
		return quantities.entrySet().stream()
				.map(entry -> new ProductSales(entry.getKey(), entry.getValue(), values.get(entry.getKey()))).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CustomerSales> customerSales(LocalDate from, LocalDate to, UUID companyId) {
		Map<UUID, BigDecimal> values = new HashMap<>();
		for (SalesOrder order : salesOrderRepositoryPort.findInvoicedByPeriod(from, to)) {
			values.merge(order.getCustomerId(), order.totalValue(), BigDecimal::add);
		}
		return values.entrySet().stream().map(entry -> new CustomerSales(entry.getKey(), entry.getValue())).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<SalespersonAchievement> targetAchievement(YearMonth month, UUID companyId) {
		Map<UUID, BigDecimal> targets = new HashMap<>();
		for (SalespersonTarget target : salespersonTargetRepositoryPort.findByMonth(month)) {
			targets.put(target.salespersonId(), target.valueTarget());
		}
		Map<UUID, BigDecimal> achieved = new HashMap<>();
		for (SalesOrder order : salesOrderRepositoryPort.findInvoicedByPeriod(month.atDay(1), month.atEndOfMonth())) {
			achieved.merge(order.getSalespersonId(), order.totalValue(), BigDecimal::add);
		}
		Map<UUID, SalespersonAchievement> bySalesperson = new HashMap<>();
		targets.forEach((salesperson, target) -> bySalesperson.put(salesperson,
				new SalespersonAchievement(salesperson, target, achieved.getOrDefault(salesperson, BigDecimal.ZERO))));
		achieved.forEach((salesperson, value) -> bySalesperson.computeIfAbsent(salesperson,
				id -> new SalespersonAchievement(id, BigDecimal.ZERO, value)));
		return List.copyOf(bySalesperson.values());
	}

	@Override
	@Transactional(readOnly = true)
	public List<CommissionRecord> commissions(LocalDate from, LocalDate to, UUID salespersonId) {
		List<SalesOrder> orders = salespersonId == null ? salesOrderRepositoryPort.findInvoicedByPeriod(from, to)
				: salesOrderRepositoryPort.findInvoicedByPeriodAndSalesperson(from, to, salespersonId);
		List<SalesOrderId> orderIds = orders.stream().map(SalesOrder::getId).toList();
		return commissionRepositoryPort.findByOrderIds(orderIds).stream().map(SalesReadModelAdapter::toRecord)
				.toList();
	}

	private static CommissionRecord toRecord(Commission commission) {
		return new CommissionRecord(commission.salespersonId(), commission.productId(), commission.orderId().value(),
				commission.rate(), commission.amount());
	}
}
