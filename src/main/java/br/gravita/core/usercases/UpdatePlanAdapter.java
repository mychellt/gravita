package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.business.UpdatePlanPort;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UpdatePlanAdapter implements UpdatePlanPort {

	private final PlanRepositoryPort planRepositoryPort;
	private final PlanCatalog planCatalog;

	public UpdatePlanAdapter(PlanRepositoryPort planRepositoryPort) {
		this.planRepositoryPort = planRepositoryPort;
		this.planCatalog = new PlanCatalog(planRepositoryPort);
	}

	@Override
	@Transactional
	public PlanDomain execute(Context context) {
		PlanDomain plan = context.getData(PlanDomain.class);
		planRepositoryPort.findById(plan.getId())
				.orElseThrow(() -> new BusinessRuleException("Plan not found: " + plan.getId()));
		return planCatalog.save(plan);
	}
}
