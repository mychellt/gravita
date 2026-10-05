package br.gravita.core.usercases;

import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.outbound.persistence.PlanRepositoryPort;

import java.util.List;

/** Writes a plan while enforcing the rules that depend on the other plans in the catalog. */
class PlanCatalog {

	private final PlanRepositoryPort planRepositoryPort;

	PlanCatalog(final PlanRepositoryPort planRepositoryPort) {
		this.planRepositoryPort = planRepositoryPort;
	}

	PlanDomain save(final PlanDomain plan) {
		plan.validate();
		final List<PlanDomain> otherPlans = planRepositoryPort.findAll().stream()
				.filter(other -> !other.getId().equals(plan.getId()))
				.toList();
		requireUniqueName(plan, otherPlans);
		requireAtLeastOneActivePlan(plan, otherPlans);
		if (plan.isFeatured()) {
			unfeature(otherPlans);
		}
		return planRepositoryPort.save(plan);
	}

	private void requireUniqueName(final PlanDomain plan, final List<PlanDomain> otherPlans) {
		final boolean nameTaken = otherPlans.stream()
				.anyMatch(other -> other.getName().strip().equalsIgnoreCase(plan.getName().strip()));
		if (nameTaken) {
			throw new BusinessRuleException("A plan named '" + plan.getName() + "' already exists");
		}
	}

	private void requireAtLeastOneActivePlan(final PlanDomain plan, final List<PlanDomain> otherPlans) {
		if (!plan.isActivePlan() && otherPlans.stream().noneMatch(PlanDomain::isActivePlan)) {
			throw new BusinessRuleException("At least one plan must remain active");
		}
	}

	private void unfeature(final List<PlanDomain> otherPlans) {
		otherPlans.stream().filter(PlanDomain::isFeatured).forEach(other -> {
			other.setFeatured(false);
			planRepositoryPort.save(other);
		});
	}
}
