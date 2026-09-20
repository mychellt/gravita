package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.PaymentMethodDomain;
import br.gravita.core.ports.outbound.persistence.commons.BaseRepositoryPort;

import java.util.UUID;

public interface PaymentMethodRepositoryPort extends BaseRepositoryPort<PaymentMethodDomain> {
	void deleteById(final UUID id);
}
