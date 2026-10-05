package br.gravita.adapters.outbound.reporting;

import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Stock carries no company, so {@code companyId} cannot narrow these reads yet. */
@PersistenceAdapter
class InventoryReadModelAdapter implements InventoryReadModelPort {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final LotRepositoryPort lotRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;
	private final StockMovementRepositoryPort stockMovementRepositoryPort;

	InventoryReadModelAdapter(final StockBalanceRepositoryPort stockBalanceRepositoryPort,
			final LotRepositoryPort lotRepositoryPort, final ProductRepositoryPort productRepositoryPort,
			final StockMovementRepositoryPort stockMovementRepositoryPort) {
		this.stockMovementRepositoryPort = stockMovementRepositoryPort;
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.lotRepositoryPort = lotRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	/**
	 * Each product's cost is the on-hand-weighted average cost over its warehouses; a product with nothing on hand
	 * falls back to the cost on its registration, and to zero when it has none.
	 */
	@Override
	@Transactional(readOnly = true)
	public BigDecimal costOfGoodsSold(final List<SoldQuantity> sold) {
		if (sold.isEmpty()) {
			return BigDecimal.ZERO;
		}
		final Map<UUID, List<StockBalance>> balancesByProduct = new HashMap<>();
		for (final StockBalance balance : stockBalanceRepositoryPort.findAll()) {
			balancesByProduct.computeIfAbsent(balance.getProductId(), id -> new ArrayList<>()).add(balance);
		}
		BigDecimal total = BigDecimal.ZERO;
		for (final SoldQuantity line : sold) {
			total = total.add(line.quantity().multiply(unitCost(line.productId(), balancesByProduct)));
		}
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StockAlert> criticalStock(final LocalDate today, final int nearExpiryDays, final UUID companyId) {
		final List<StockAlert> alerts = new ArrayList<>();
		final Map<UUID, BigDecimal> availableByProduct = new HashMap<>();
		for (final StockBalance balance : stockBalanceRepositoryPort.findAll()) {
			availableByProduct.merge(balance.getProductId(), balance.available(), BigDecimal::add);
		}
		availableByProduct.forEach((productId, available) -> productRepositoryPort.get(productId)
				.map(ProductDomain::getStock).map(StockParametersDomain::minimum)
				.filter(minimum -> available.compareTo(minimum) < 0)
				.ifPresent(minimum -> alerts.add(new StockAlert(productId, available, minimum, null, null, null))));
		for (final Lot lot : lotRepositoryPort.findByExpiryDateLessThanEqual(today.plusDays(nearExpiryDays))) {
			if (lot.getQuantity().signum() > 0) {
				alerts.add(new StockAlert(lot.getProductId(), null, null, lot.getWarehouseId(), lot.getCode(),
						lot.getExpiryDate()));
			}
		}
		return alerts;
	}

	/**
	 * Levels are rebuilt backwards from today's balances: what came in or out after the period is undone to get the
	 * closing level, and what moved within it to get the opening one. An entry adds, an exit takes away, an
	 * adjustment carries its own signed delta; a transfer only moves stock between warehouses, so it changes
	 * neither the total on hand nor what was issued.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<StockFlow> stockFlows(final LocalDate from, final LocalDate to, final UUID companyId) {
		final Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
		final Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		final Map<UUID, BigDecimal> onHandNow = new HashMap<>();
		for (final StockBalance balance : stockBalanceRepositoryPort.findAll()) {
			onHandNow.merge(balance.getProductId(), balance.getOnHand(), BigDecimal::add);
		}
		final Map<UUID, BigDecimal> issued = new HashMap<>();
		final Map<UUID, BigDecimal> netInPeriod = new HashMap<>();
		final Map<UUID, BigDecimal> netAfterPeriod = new HashMap<>();
		for (final StockMovement movement : stockMovementRepositoryPort.findByTimestampGreaterThanEqual(start)) {
			final BigDecimal change = stockChange(movement);
			if (movement.getTimestamp().isBefore(end)) {
				netInPeriod.merge(movement.getProductId(), change, BigDecimal::add);
				if (movement.getType() == StockMovementType.EXIT) {
					issued.merge(movement.getProductId(), change.negate(), BigDecimal::add);
				}
			} else {
				netAfterPeriod.merge(movement.getProductId(), change, BigDecimal::add);
			}
		}
		final Set<UUID> products = new TreeSet<>(onHandNow.keySet());
		products.addAll(netInPeriod.keySet());
		final List<StockFlow> flows = new ArrayList<>(products.size());
		for (final UUID productId : products) {
			final BigDecimal closing = onHandNow.getOrDefault(productId, BigDecimal.ZERO)
					.subtract(netAfterPeriod.getOrDefault(productId, BigDecimal.ZERO));
			final BigDecimal opening = closing.subtract(netInPeriod.getOrDefault(productId, BigDecimal.ZERO));
			flows.add(new StockFlow(productId, issued.getOrDefault(productId, BigDecimal.ZERO), opening, closing));
		}
		return flows;
	}

	private BigDecimal stockChange(final StockMovement movement) {
		return switch (movement.getType()) {
			case ENTRY, ADJUSTMENT -> movement.getQuantity();
			case EXIT -> movement.getQuantity().negate();
			case TRANSFER -> BigDecimal.ZERO;
		};
	}

	private BigDecimal unitCost(final UUID productId, final Map<UUID, List<StockBalance>> balancesByProduct) {
		final List<StockBalance> balances = balancesByProduct.getOrDefault(productId, List.of());
		final BigDecimal onHand = balances.stream().map(StockBalance::getOnHand).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (onHand.signum() > 0) {
			final BigDecimal value = balances.stream().map(balance -> balance.getOnHand().multiply(balance.getAverageCost()))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			return value.divide(onHand, 4, RoundingMode.HALF_UP);
		}
		return productRepositoryPort.get(productId).map(ProductDomain::getAverageCost).orElse(BigDecimal.ZERO);
	}
}
