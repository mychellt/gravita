package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import java.util.Optional;

public interface InboundNfeRepositoryPort {
	InboundNfe save(InboundNfe inboundNfe);

	Optional<InboundNfe> findById(InboundNfeId id);
}
