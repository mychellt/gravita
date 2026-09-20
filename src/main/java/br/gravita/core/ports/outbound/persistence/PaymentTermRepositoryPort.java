package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.PaymentTermDomain;
import br.gravita.core.ports.outbound.persistence.commons.BaseRepositoryPort;

import java.util.UUID;

public interface PaymentTermRepositoryPort extends BaseRepositoryPort<PaymentTermDomain> {
	void deleteById(final UUID id);
}
