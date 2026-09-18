package br.gravita.core.ports.persistence;

import br.gravita.core.domain.PaymentMethodDomain;
import br.gravita.core.ports.persistence.commons.BaseRepositoryPort;

import java.util.UUID;

public interface PaymentMethodRepositoryPort extends BaseRepositoryPort<PaymentMethodDomain> {
	void deleteById(final UUID id);
}
