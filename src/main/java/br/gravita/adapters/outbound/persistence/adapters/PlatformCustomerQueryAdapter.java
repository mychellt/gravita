package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.PaymentJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.SubscriptionJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.SubscriptionJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.ports.inbound.system.PlatformCustomerView;
import br.gravita.core.ports.inbound.system.PlatformPaymentView;
import br.gravita.core.ports.outbound.persistence.system.PlatformCustomerQueryPort;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
class PlatformCustomerQueryAdapter implements PlatformCustomerQueryPort {

	private final SubscriptionJpaRepository subscriptionJpaRepository;

	PlatformCustomerQueryAdapter(final SubscriptionJpaRepository subscriptionJpaRepository) {
		this.subscriptionJpaRepository = subscriptionJpaRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PlatformCustomerView> findAllSubscribers() {
		return subscriptionJpaRepository.findAllByOrderByActivationDateDesc().stream().map(this::toView).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PlatformPaymentView> findPaymentsByCompanyId(final UUID companyId) {
		return subscriptionJpaRepository.findAllByCompanyId(companyId).stream()
				.flatMap(subscription -> subscription.getPayments().stream())
				.filter(Objects::nonNull)
				.map(this::toView)
				.toList();
	}

	private PlatformCustomerView toView(final SubscriptionJpaEntity subscription) {
		return new PlatformCustomerView(subscription.getCompany().getId(), subscription.getCompany().getDocument(),
				subscription.getCompany().getName(), subscription.getCompany().getIssuingEmail(),
				subscription.getCompany().getPhone(), subscription.getPlan().getTier(), subscription.getPlan().getName(),
				subscription.getStatus(), subscription.getActivationDate(), subscription.getExpirationDate());
	}

	private PlatformPaymentView toView(final PaymentJpaEntity payment) {
		return new PlatformPaymentView(payment.getId(), payment.getAmount(), payment.getPaymentDate());
	}
}
