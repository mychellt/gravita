package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.domain.sales.StageTransition;
import br.gravita.core.domain.sales.StageTransitionId;
import br.gravita.core.ports.inbound.sales.FunnelConversionView;
import br.gravita.core.ports.inbound.sales.FunnelConversionView.StageConversionRate;
import br.gravita.core.ports.inbound.sales.GetFunnelConversionQuery;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.StageTransitionRepositoryPort;
import br.gravita.core.usercases.sales.GetFunnelConversionService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetFunnelConversionServiceTest {

	@Mock
	private OpportunityRepositoryPort opportunityRepositoryPort;

	@Mock
	private StageTransitionRepositoryPort stageTransitionRepositoryPort;

	@InjectMocks
	private GetFunnelConversionService service;

	private static final YearMonth PERIOD = YearMonth.of(2026, 9);
	private static final Instant PERIOD_START = PERIOD.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
	private static final Instant PERIOD_END = PERIOD.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

	@Test
	void computesConversionRatePerStageTransitionAndVolumePerSalesperson() {
		UUID salespersonA = UUID.randomUUID();
		UUID salespersonB = UUID.randomUUID();
		OpportunityId opp1 = OpportunityId.of(UUID.randomUUID());
		OpportunityId opp2 = OpportunityId.of(UUID.randomUUID());
		OpportunityId opp3 = OpportunityId.of(UUID.randomUUID());

		List<StageTransition> transitions = List.of(
				transition(opp1, OpportunityStage.PROSPECTING, OpportunityStage.PROPOSAL,
						PERIOD_START.plusSeconds(60)),
				transition(opp2, OpportunityStage.PROSPECTING, OpportunityStage.PROPOSAL,
						PERIOD_START.plusSeconds(120)),
				transition(opp2, OpportunityStage.PROPOSAL, OpportunityStage.NEGOTIATION,
						PERIOD_START.plusSeconds(180)),
				transition(opp3, OpportunityStage.PROSPECTING, OpportunityStage.LOST, PERIOD_START.plusSeconds(240)));
		when(stageTransitionRepositoryPort.findByPeriod(PERIOD_START, PERIOD_END)).thenReturn(transitions);
		when(opportunityRepositoryPort.findByIds(Set.of(opp1, opp2, opp3))).thenReturn(List.of(
				opportunity(opp1, salespersonA), opportunity(opp2, salespersonA), opportunity(opp3, salespersonB)));

		FunnelConversionView view = service.execute(new GetFunnelConversionQuery(PERIOD, null));

		assertThat(view.conversionRateByStage()).containsExactlyInAnyOrder(
				new StageConversionRate(OpportunityStage.PROSPECTING, OpportunityStage.PROPOSAL,
						new BigDecimal("0.6667")),
				new StageConversionRate(OpportunityStage.PROSPECTING, OpportunityStage.LOST,
						new BigDecimal("0.3333")),
				new StageConversionRate(OpportunityStage.PROPOSAL, OpportunityStage.NEGOTIATION,
						new BigDecimal("1.0000")));
		assertThat(view.volumeBySalesperson()).containsEntry(salespersonA, 2L).containsEntry(salespersonB, 1L);
		assertThat(view.averageCycleTime()).isEqualTo(Duration.ZERO);
	}

	@Test
	void filtersToTheGivenSalespersonWhenProvided() {
		UUID salespersonA = UUID.randomUUID();
		UUID salespersonB = UUID.randomUUID();
		OpportunityId opp1 = OpportunityId.of(UUID.randomUUID());
		OpportunityId opp2 = OpportunityId.of(UUID.randomUUID());

		List<StageTransition> transitions = List.of(
				transition(opp1, OpportunityStage.PROSPECTING, OpportunityStage.PROPOSAL,
						PERIOD_START.plusSeconds(60)),
				transition(opp2, OpportunityStage.PROSPECTING, OpportunityStage.LOST, PERIOD_START.plusSeconds(120)));
		when(stageTransitionRepositoryPort.findByPeriod(PERIOD_START, PERIOD_END)).thenReturn(transitions);
		when(opportunityRepositoryPort.findByIds(Set.of(opp1, opp2)))
				.thenReturn(List.of(opportunity(opp1, salespersonA), opportunity(opp2, salespersonB)));

		FunnelConversionView view = service.execute(new GetFunnelConversionQuery(PERIOD, salespersonA));

		assertThat(view.volumeBySalesperson()).containsOnly(Map.entry(salespersonA, 1L));
		assertThat(view.conversionRateByStage()).containsExactly(new StageConversionRate(OpportunityStage.PROSPECTING,
				OpportunityStage.PROPOSAL, new BigDecimal("1.0000")));
	}

	@Test
	void averageCycleTimeIsComputedFromTheFirstTransitionToTheClosingTransition() {
		UUID salesperson = UUID.randomUUID();
		OpportunityId opportunityId = OpportunityId.of(UUID.randomUUID());
		Instant firstTransitionAt = PERIOD_START.minusSeconds(3600);
		Instant closingAt = PERIOD_START.plusSeconds(1800);

		StageTransition firstTransition = transition(opportunityId, OpportunityStage.PROSPECTING,
				OpportunityStage.PROPOSAL, firstTransitionAt);
		StageTransition closingTransition = transition(opportunityId, OpportunityStage.NEGOTIATION,
				OpportunityStage.CLOSED, closingAt);

		when(stageTransitionRepositoryPort.findByPeriod(PERIOD_START, PERIOD_END))
				.thenReturn(List.of(closingTransition));
		when(stageTransitionRepositoryPort.findByOpportunityId(opportunityId))
				.thenReturn(List.of(firstTransition, closingTransition));
		when(opportunityRepositoryPort.findByIds(Set.of(opportunityId)))
				.thenReturn(List.of(opportunity(opportunityId, salesperson)));

		FunnelConversionView view = service.execute(new GetFunnelConversionQuery(PERIOD, null));

		assertThat(view.averageCycleTime()).isEqualTo(Duration.between(firstTransitionAt, closingAt));
	}

	@Test
	void noTransitionsInThePeriodYieldsEmptyResultsRatherThanAnError() {
		when(stageTransitionRepositoryPort.findByPeriod(PERIOD_START, PERIOD_END)).thenReturn(List.of());
		when(opportunityRepositoryPort.findByIds(Set.of())).thenReturn(List.of());

		FunnelConversionView view = service.execute(new GetFunnelConversionQuery(PERIOD, null));

		assertThat(view.conversionRateByStage()).isEmpty();
		assertThat(view.volumeBySalesperson()).isEmpty();
		assertThat(view.averageCycleTime()).isEqualTo(Duration.ZERO);
	}

	private static StageTransition transition(OpportunityId opportunityId, OpportunityStage from,
			OpportunityStage to, Instant timestamp) {
		return StageTransition.of(StageTransitionId.of(UUID.randomUUID()), opportunityId, from, to, timestamp);
	}

	private static Opportunity opportunity(OpportunityId id, UUID owner) {
		return Opportunity.of(id, UUID.randomUUID(), BigDecimal.valueOf(1000), 50, LocalDate.now().plusDays(10),
				owner, OpportunityStage.PROSPECTING);
	}
}
