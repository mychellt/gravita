package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.ports.inbound.sales.FunnelConversionView;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FunnelConversionResponse(List<StageConversionRateResponse> conversionRateByStage,
		Duration averageCycleTime, Map<UUID, Long> volumeBySalesperson) {

	public record StageConversionRateResponse(OpportunityStage fromStage, OpportunityStage toStage,
			BigDecimal conversionRate) {

		public static StageConversionRateResponse from(FunnelConversionView.StageConversionRate rate) {
			return new StageConversionRateResponse(rate.fromStage(), rate.toStage(), rate.conversionRate());
		}
	}

	public static FunnelConversionResponse from(FunnelConversionView view) {
		List<StageConversionRateResponse> rates = view.conversionRateByStage().stream()
				.map(StageConversionRateResponse::from)
				.toList();
		return new FunnelConversionResponse(rates, view.averageCycleTime(), view.volumeBySalesperson());
	}
}
