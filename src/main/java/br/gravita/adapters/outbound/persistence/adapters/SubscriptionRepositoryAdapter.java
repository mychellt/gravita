package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.SubscriptionJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.CompanyPersonJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.PlanJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.SubscriptionJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.Subscription;
import br.gravita.core.ports.outbound.persistence.SubscriptionRepositoryPort;

import java.util.ArrayList;

@PersistenceAdapter
class SubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {

	private final SubscriptionJpaRepository jpaRepository;
	private final PlanJpaRepository planJpaRepository;
	private final CompanyPersonJpaRepository companyJpaRepository;

	SubscriptionRepositoryAdapter(SubscriptionJpaRepository jpaRepository, PlanJpaRepository planJpaRepository,
			CompanyPersonJpaRepository companyJpaRepository) {
		this.jpaRepository = jpaRepository;
		this.planJpaRepository = planJpaRepository;
		this.companyJpaRepository = companyJpaRepository;
	}

	@Override
	public Subscription save(Subscription subscription) {
		SubscriptionJpaEntity entity = SubscriptionJpaEntity.builder()
				.plan(planJpaRepository.getReferenceById(subscription.getPlan().getId()))
				.person(companyJpaRepository.getReferenceById(subscription.getPerson().getId()))
				.billingCycle(subscription.getBillingCycle())
				.status(subscription.getStatus())
				.activationDate(subscription.getActivationDate())
				.expirationDate(subscription.getExpirationDate())
				.payments(new ArrayList<>())
				.build();
		subscription.setId(jpaRepository.save(entity).getId());
		return subscription;
	}
}
