package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.OpportunityStage;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FunnelConversionView(List<StageConversionRate> conversionRateByStage, Duration averageCycleTime,
		Map<UUID, Long> volumeBySalesperson) {

	public record StageConversionRate(OpportunityStage fromStage, OpportunityStage toStage,
			BigDecimal conversionRate) {
	}
}
