package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.ports.inbound.sales.TargetProgressView;
import java.math.BigDecimal;

public record TargetProgressResponse(BigDecimal valueAchieved, long orderCountAchieved, BigDecimal valueTarget,
		long orderCountTarget, PercentCompleteResponse percentComplete, boolean targetConfigured) {

	public record PercentCompleteResponse(BigDecimal value, BigDecimal orderCount) {

		static PercentCompleteResponse from(final TargetProgressView.PercentComplete percentComplete) {
			return percentComplete == null ? null
					: new PercentCompleteResponse(percentComplete.value(), percentComplete.orderCount());
		}
	}

	public static TargetProgressResponse from(final TargetProgressView view) {
		return new TargetProgressResponse(view.valueAchieved(), view.orderCountAchieved(), view.valueTarget(),
				view.orderCountTarget(), PercentCompleteResponse.from(view.percentComplete()),
				view.targetConfigured());
	}
}
