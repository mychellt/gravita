package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.StockBalance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public record StockBalanceView(UUID productId, UUID warehouseId, BigDecimal onHand, BigDecimal reserved,
		BigDecimal inTransit, BigDecimal available, BigDecimal averageCost) {

	public static StockBalanceView zero(final UUID productId, final UUID warehouseId) {
		return new StockBalanceView(productId, warehouseId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO);
	}

	public static StockBalanceView from(final UUID productId, final UUID warehouseId, final StockBalance balance) {
		return new StockBalanceView(productId, warehouseId, balance.getOnHand(), balance.getReserved(),
				balance.getInTransit(), balance.available(), balance.getAverageCost());
	}

	public static StockBalanceView aggregate(final UUID productId, final List<StockBalance> balances) {
		if (balances.isEmpty()) {
			return zero(productId, null);
		}
		final BigDecimal onHand = sum(balances, StockBalance::getOnHand);
		final BigDecimal reserved = sum(balances, StockBalance::getReserved);
		final BigDecimal inTransit = sum(balances, StockBalance::getInTransit);
		final BigDecimal averageCost = weightedAverageCost(balances, onHand);
		return new StockBalanceView(productId, null, onHand, reserved, inTransit, onHand.subtract(reserved),
				averageCost);
	}

	private static BigDecimal sum(final List<StockBalance> balances, final Function<StockBalance, BigDecimal> extractor) {
		return balances.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal weightedAverageCost(final List<StockBalance> balances, final BigDecimal totalOnHand) {
		if (totalOnHand.signum() == 0) {
			return BigDecimal.ZERO;
		}
		final BigDecimal weightedSum = balances.stream()
				.map(balance -> balance.getAverageCost().multiply(balance.getOnHand()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return weightedSum.divide(totalOnHand, 4, RoundingMode.HALF_UP);
	}
}
