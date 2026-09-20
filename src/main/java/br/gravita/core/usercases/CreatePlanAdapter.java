package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.ports.business.CreatePlanPort;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreatePlanAdapter implements CreatePlanPort {

	private final PlanRepositoryPort planRepositoryPort;

	public CreatePlanAdapter(PlanRepositoryPort planRepositoryPort) {
		this.planRepositoryPort = planRepositoryPort;
	}

	@Override
	public PlanDomain execute(Context context) {
		PlanDomain plan = context.getData(PlanDomain.class);
		if (plan.getId() == null) {
			plan.setId(UUID.randomUUID());
		}
		return planRepositoryPort.save(plan);
	}
}
