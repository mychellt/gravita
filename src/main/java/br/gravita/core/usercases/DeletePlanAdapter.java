package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.business.DeletePlanPort;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeletePlanAdapter implements DeletePlanPort {

	private final PlanRepositoryPort planRepositoryPort;

	public DeletePlanAdapter(PlanRepositoryPort planRepositoryPort) {
		this.planRepositoryPort = planRepositoryPort;
	}

	@Override
	public Void execute(Context context) {
		UUID id = context.getData(UUID.class);
		planRepositoryPort.findById(id)
				.orElseThrow(() -> new BusinessRuleException("Plan not found: " + id));
		planRepositoryPort.deleteById(id);
		return null;
	}
}
