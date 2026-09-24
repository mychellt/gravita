package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.InboundNfe;

public interface InboundNfeRepositoryPort {
	InboundNfe save(InboundNfe inboundNfe);
}
