package br.gravita.adapters.outbound.reporting;

import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Stock carries no company, so {@code companyId} cannot narrow these reads yet. */
@PersistenceAdapter
class InventoryReadModelAdapter implements InventoryReadModelPort {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final LotRepositoryPort lotRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	InventoryReadModelAdapter(StockBalanceRepositoryPort stockBalanceRepositoryPort,
			LotRepositoryPort lotRepositoryPort, ProductRepositoryPort productRepositoryPort) {
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
	public BigDecimal costOfGoodsSold(List<SoldQuantity> sold) {
		if (sold.isEmpty()) {
			return BigDecimal.ZERO;
		}
		Map<UUID, List<StockBalance>> balancesByProduct = new HashMap<>();
		for (StockBalance balance : stockBalanceRepositoryPort.findAll()) {
			balancesByProduct.computeIfAbsent(balance.getProductId(), id -> new ArrayList<>()).add(balance);
		}
		BigDecimal total = BigDecimal.ZERO;
		for (SoldQuantity line : sold) {
			total = total.add(line.quantity().multiply(unitCost(line.productId(), balancesByProduct)));
		}
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StockAlert> criticalStock(LocalDate today, int nearExpiryDays, UUID companyId) {
		List<StockAlert> alerts = new ArrayList<>();
		Map<UUID, BigDecimal> availableByProduct = new HashMap<>();
		for (StockBalance balance : stockBalanceRepositoryPort.findAll()) {
			availableByProduct.merge(balance.getProductId(), balance.available(), BigDecimal::add);
		}
		availableByProduct.forEach((productId, available) -> productRepositoryPort.get(productId)
				.map(ProductDomain::getStock).map(StockParametersDomain::minimum)
				.filter(minimum -> available.compareTo(minimum) < 0)
				.ifPresent(minimum -> alerts.add(new StockAlert(productId, available, minimum, null, null, null))));
		for (Lot lot : lotRepositoryPort.findByExpiryDateLessThanEqual(today.plusDays(nearExpiryDays))) {
			if (lot.getQuantity().signum() > 0) {
				alerts.add(new StockAlert(lot.getProductId(), null, null, lot.getWarehouseId(), lot.getCode(),
						lot.getExpiryDate()));
			}
		}
		return alerts;
	}

	private BigDecimal unitCost(UUID productId, Map<UUID, List<StockBalance>> balancesByProduct) {
		List<StockBalance> balances = balancesByProduct.getOrDefault(productId, List.of());
		BigDecimal onHand = balances.stream().map(StockBalance::getOnHand).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (onHand.signum() > 0) {
			BigDecimal value = balances.stream().map(balance -> balance.getOnHand().multiply(balance.getAverageCost()))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			return value.divide(onHand, 4, RoundingMode.HALF_UP);
		}
		return productRepositoryPort.get(productId).map(ProductDomain::getAverageCost).orElse(BigDecimal.ZERO);
	}
}
