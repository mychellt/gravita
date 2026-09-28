package br.gravita.core.ports.inbound.sales;

import java.math.BigDecimal;

public record TargetProgressView(BigDecimal valueAchieved, long orderCountAchieved, BigDecimal valueTarget,
		long orderCountTarget, PercentComplete percentComplete, boolean targetConfigured) {

	public record PercentComplete(BigDecimal value, BigDecimal orderCount) {
	}
}
