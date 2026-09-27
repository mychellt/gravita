package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import java.util.Optional;

public interface InboundNfeRepositoryPort {
	InboundNfe save(InboundNfe inboundNfe);

	Optional<InboundNfe> findById(InboundNfeId id);

	/**
	 * UC-M2-07: a soft lookup used to link a manifestation back to an
	 * already-imported record when one happens to exist - never required, since
	 * manifestation works by access key alone regardless of import status.
	 */
	Optional<InboundNfe> findByAccessKey(String accessKey);
}
