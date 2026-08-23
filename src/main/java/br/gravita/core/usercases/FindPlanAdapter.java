package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.business.FindPlanPort;
import br.gravita.core.ports.persistence.PlanRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FindPlanAdapter implements FindPlanPort {

	private final PlanRepositoryPort planRepositoryPort;

	public FindPlanAdapter(PlanRepositoryPort planRepositoryPort) {
		this.planRepositoryPort = planRepositoryPort;
	}

	@Override
	public PlanDomain execute(Context context) {
		UUID id = context.getData(UUID.class);
		return planRepositoryPort.findById(id)
				.orElseThrow(() -> new BusinessRuleException("Plan not found: " + id));
	}
}
