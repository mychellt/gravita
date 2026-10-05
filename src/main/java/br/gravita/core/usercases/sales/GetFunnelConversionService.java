package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.domain.sales.StageTransition;
import br.gravita.core.ports.inbound.sales.FunnelConversionView;
import br.gravita.core.ports.inbound.sales.FunnelConversionView.StageConversionRate;
import br.gravita.core.ports.inbound.sales.GetFunnelConversionQuery;
import br.gravita.core.ports.inbound.sales.GetFunnelConversionUseCase;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.StageTransitionRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetFunnelConversionService implements GetFunnelConversionUseCase {

	private final OpportunityRepositoryPort opportunityRepositoryPort;
	private final StageTransitionRepositoryPort stageTransitionRepositoryPort;

	@Override
	public FunnelConversionView execute(final GetFunnelConversionQuery query) {
		final Instant periodStart = query.period().atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		final Instant periodEnd = query.period().plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

		List<StageTransition> transitions = stageTransitionRepositoryPort.findByPeriod(periodStart, periodEnd);

		final Set<OpportunityId> opportunityIds = transitions.stream().map(StageTransition::getOpportunityId)
				.collect(Collectors.toSet());
		final Map<OpportunityId, UUID> ownerByOpportunity = opportunityRepositoryPort.findByIds(opportunityIds).stream()
				.collect(Collectors.toMap(Opportunity::getId, Opportunity::getOwner));

		if (query.salesperson() != null) {
			transitions = transitions.stream()
					.filter(transition -> query.salesperson().equals(ownerByOpportunity.get(transition.getOpportunityId())))
					.toList();
		}

		return new FunnelConversionView(conversionRateByStage(transitions), averageCycleTime(transitions),
				volumeBySalesperson(transitions, ownerByOpportunity));
	}

	private List<StageConversionRate> conversionRateByStage(final List<StageTransition> transitions) {
		final Map<OpportunityStage, Long> exitsByFromStage = transitions.stream()
				.collect(Collectors.groupingBy(StageTransition::getFromStage, Collectors.counting()));

		final Map<Map.Entry<OpportunityStage, OpportunityStage>, Long> countByTransition = transitions.stream()
				.collect(Collectors.groupingBy(transition -> Map.entry(transition.getFromStage(), transition.getToStage()),
						Collectors.counting()));

		return countByTransition.entrySet().stream()
				.map(entry -> {
					final OpportunityStage fromStage = entry.getKey().getKey();
					final OpportunityStage toStage = entry.getKey().getValue();
					final BigDecimal rate = BigDecimal.valueOf(entry.getValue())
							.divide(BigDecimal.valueOf(exitsByFromStage.get(fromStage)), 4, RoundingMode.HALF_UP);
					return new StageConversionRate(fromStage, toStage, rate);
				})
				.sorted(Comparator.comparing(StageConversionRate::fromStage).thenComparing(StageConversionRate::toStage))
				.toList();
	}

	private Duration averageCycleTime(final List<StageTransition> transitions) {
		final List<StageTransition> closedInPeriod = transitions.stream()
				.filter(transition -> transition.getToStage() == OpportunityStage.CLOSED)
				.toList();
		if (closedInPeriod.isEmpty()) {
			return Duration.ZERO;
		}

		final List<Duration> cycleTimes = closedInPeriod.stream().map(this::cycleTimeToClose).toList();
		final Duration total = cycleTimes.stream().reduce(Duration.ZERO, Duration::plus);
		return total.dividedBy(cycleTimes.size());
	}

	private Duration cycleTimeToClose(final StageTransition closingTransition) {
		final Instant firstTransitionAt = stageTransitionRepositoryPort
				.findByOpportunityId(closingTransition.getOpportunityId()).stream()
				.map(StageTransition::getTimestamp)
				.min(Comparator.naturalOrder())
				.orElse(closingTransition.getTimestamp());
		return Duration.between(firstTransitionAt, closingTransition.getTimestamp());
	}

	private Map<UUID, Long> volumeBySalesperson(final List<StageTransition> transitions,
			final Map<OpportunityId, UUID> ownerByOpportunity) {
		return transitions.stream()
				.map(StageTransition::getOpportunityId)
				.distinct()
				.collect(Collectors.groupingBy(ownerByOpportunity::get, LinkedHashMap::new, Collectors.counting()));
	}
}
