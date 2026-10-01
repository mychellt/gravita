package br.gravita.core.usercases.reporting;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.ports.inbound.reporting.GetSupplierPurchaseSummaryUseCase;
import br.gravita.core.ports.inbound.reporting.SupplierPurchaseSummary;
import br.gravita.core.ports.inbound.reporting.SupplierPurchaseSummaryQuery;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.PurchasingReadModelPort;
import br.gravita.core.ports.outbound.reporting.PurchasingReadModelPort.PurchasedOrder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Purchases per supplier for the month (doc §10.2): the quantity and value ordered, and the average lead time. Every
 * confirmed receipt of an order placed in the month is one delivery, measured in calendar days from the order to the
 * confirmation, so an order delivered in two parts counts twice; the average is over deliveries, not orders. A
 * supplier none of whose orders has been received yet is reported with no lead time. Suppliers are ranked by the
 * value bought, largest first.
 */
@UseCase
public class GetSupplierPurchaseSummaryService implements GetSupplierPurchaseSummaryUseCase {

	static final String SCREEN = "purchases-by-supplier";

	private final PurchasingReadModelPort purchasingReadModelPort;
	private final PermissionCheckPort permissionCheckPort;

	public GetSupplierPurchaseSummaryService(PurchasingReadModelPort purchasingReadModelPort,
			PermissionCheckPort permissionCheckPort) {
		this.purchasingReadModelPort = purchasingReadModelPort;
		this.permissionCheckPort = permissionCheckPort;
	}

	@Override
	public List<SupplierPurchaseSummary> execute(SupplierPurchaseSummaryQuery query) {
		if (!permissionCheckPort.canView(query.requesterId(), SCREEN)) {
			throw new ForbiddenException("The user's profile cannot view the purchases by supplier report");
		}
		List<PurchasedOrder> orders = purchasingReadModelPort.purchasedOrders(query.period().atDay(1),
				query.period().atEndOfMonth(), query.companyId());
		Map<UUID, List<PurchasedOrder>> bySupplier = new LinkedHashMap<>();
		for (PurchasedOrder order : orders) {
			bySupplier.computeIfAbsent(order.supplierId(), id -> new ArrayList<>()).add(order);
		}
		Comparator<SupplierPurchaseSummary> ranking = Comparator.comparing(SupplierPurchaseSummary::value).reversed()
				.thenComparing(SupplierPurchaseSummary::supplier);
		return bySupplier.entrySet().stream().map(entry -> summary(entry.getKey(), entry.getValue())).sorted(ranking)
				.toList();
	}

	private SupplierPurchaseSummary summary(UUID supplier, List<PurchasedOrder> orders) {
		BigDecimal volume = orders.stream().map(PurchasedOrder::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal value = orders.stream().map(PurchasedOrder::value).reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
		return new SupplierPurchaseSummary(supplier, volume, value, averageLeadTimeDays(orders));
	}

	private BigDecimal averageLeadTimeDays(List<PurchasedOrder> orders) {
		long days = 0;
		long deliveries = 0;
		for (PurchasedOrder order : orders) {
			for (LocalDate receivedOn : order.receivedOn()) {
				days += ChronoUnit.DAYS.between(order.orderedOn(), receivedOn);
				deliveries++;
			}
		}
		if (deliveries == 0) {
			return null;
		}
		return BigDecimal.valueOf(days).divide(BigDecimal.valueOf(deliveries), 2, RoundingMode.HALF_UP);
	}
}
