package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.Subscription;

public interface SubscriptionRepositoryPort {
	Subscription save(Subscription subscription);
}
