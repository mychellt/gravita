package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.ports.business.ListPlansPort;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListPlansAdapter implements ListPlansPort {

	private final PlanRepositoryPort planRepositoryPort;

	public ListPlansAdapter(PlanRepositoryPort planRepositoryPort) {
		this.planRepositoryPort = planRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PlanDomain> execute(Context context) {
		return planRepositoryPort.findAll();
	}
}
