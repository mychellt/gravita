package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.SubscriptionJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.PlanJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.SubscriptionJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.CompanyJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.Subscription;
import br.gravita.core.ports.outbound.persistence.SubscriptionRepositoryPort;

import java.util.ArrayList;

@PersistenceAdapter
class SubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {

	private final SubscriptionJpaRepository jpaRepository;
	private final PlanJpaRepository planJpaRepository;
	private final CompanyJpaRepository companyJpaRepository;

	SubscriptionRepositoryAdapter(final SubscriptionJpaRepository jpaRepository, final PlanJpaRepository planJpaRepository,
			final CompanyJpaRepository companyJpaRepository) {
		this.jpaRepository = jpaRepository;
		this.planJpaRepository = planJpaRepository;
		this.companyJpaRepository = companyJpaRepository;
	}

	@Override
	public Subscription save(final Subscription subscription) {
		final SubscriptionJpaEntity entity = SubscriptionJpaEntity.builder()
				.plan(planJpaRepository.getReferenceById(subscription.getPlan().getId()))
				.company(companyJpaRepository.getReferenceById(subscription.getCompany().getId().value()))
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
