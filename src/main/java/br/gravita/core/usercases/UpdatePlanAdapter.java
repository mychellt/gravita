package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.business.UpdatePlanPort;
import br.gravita.core.ports.persistence.PlanRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class UpdatePlanAdapter implements UpdatePlanPort {

	private final PlanRepositoryPort planRepositoryPort;

	public UpdatePlanAdapter(PlanRepositoryPort planRepositoryPort) {
		this.planRepositoryPort = planRepositoryPort;
	}

	@Override
	public PlanDomain execute(Context context) {
		PlanDomain plan = context.getData(PlanDomain.class);
		planRepositoryPort.findById(plan.getId())
				.orElseThrow(() -> new BusinessRuleException("Plan not found: " + plan.getId()));
		return planRepositoryPort.save(plan);
	}
}
