package br.gravita.adapters.inbound.scheduling.sales;

import br.gravita.core.ports.inbound.sales.EvaluateFollowUpRulesUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives {@link EvaluateFollowUpRulesUseCase} (UC-M7-12) on a schedule - the use case has no REST
 * endpoint, so this poll loop is its only trigger.
 */
@Component
public class FollowUpRuleEvaluationScheduler {

	private final EvaluateFollowUpRulesUseCase evaluateFollowUpRulesUseCase;

	public FollowUpRuleEvaluationScheduler(final EvaluateFollowUpRulesUseCase evaluateFollowUpRulesUseCase) {
		this.evaluateFollowUpRulesUseCase = evaluateFollowUpRulesUseCase;
	}

	@Scheduled(cron = "${gravita.sales.follow-up-rules.evaluation-cron:0 0 7 * * *}")
	public void evaluate() {
		evaluateFollowUpRulesUseCase.execute();
	}
}
