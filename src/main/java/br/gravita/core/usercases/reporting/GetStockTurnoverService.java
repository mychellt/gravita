package br.gravita.core.usercases.reporting;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.ports.inbound.reporting.GetStockTurnoverUseCase;
import br.gravita.core.ports.inbound.reporting.StockTurnoverEntry;
import br.gravita.core.ports.inbound.reporting.StockTurnoverQuery;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort.StockFlow;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Turnover per product for the month (doc §10.2): the quantity issued divided by the average of the opening and
 * closing stock, so a product that sold its average stock twice over scores 2. A product that held stock but
 * issued none of it is flagged as stalled; one that held none and issued none is not reported at all.
 */
@UseCase
public class GetStockTurnoverService implements GetStockTurnoverUseCase {

	static final String SCREEN = "stock-turnover";

	private static final BigDecimal TWO = BigDecimal.valueOf(2);

	private final InventoryReadModelPort inventoryReadModelPort;
	private final PermissionCheckPort permissionCheckPort;

	public GetStockTurnoverService(final InventoryReadModelPort inventoryReadModelPort,
			final PermissionCheckPort permissionCheckPort) {
		this.inventoryReadModelPort = inventoryReadModelPort;
		this.permissionCheckPort = permissionCheckPort;
	}

	@Override
	public List<StockTurnoverEntry> execute(final StockTurnoverQuery query) {
		if (!permissionCheckPort.canView(query.requesterId(), SCREEN)) {
			throw new ForbiddenException("The user's profile cannot view the stock turnover report");
		}
		final List<StockFlow> flows = inventoryReadModelPort.stockFlows(query.period().atDay(1),
				query.period().atEndOfMonth(), query.companyId());
		final Comparator<StockTurnoverEntry> order = Comparator
				.comparing(StockTurnoverEntry::turnoverRate, Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(StockTurnoverEntry::product);
		return flows.stream().filter(flow -> held(flow) || issued(flow)).map(this::entry).sorted(order).toList();
	}

	private StockTurnoverEntry entry(final StockFlow flow) {
		final BigDecimal average = averageStock(flow);
		final BigDecimal rate = average.signum() > 0 ? flow.issued().divide(average, 4, RoundingMode.HALF_UP) : null;
		return new StockTurnoverEntry(flow.productId(), rate, !issued(flow));
	}

	private BigDecimal averageStock(final StockFlow flow) {
		return flow.openingOnHand().add(flow.closingOnHand()).divide(TWO, 4, RoundingMode.HALF_UP);
	}

	private boolean held(final StockFlow flow) {
		return flow.openingOnHand().signum() > 0 || flow.closingOnHand().signum() > 0;
	}

	private boolean issued(final StockFlow flow) {
		return flow.issued().signum() > 0;
	}
}
