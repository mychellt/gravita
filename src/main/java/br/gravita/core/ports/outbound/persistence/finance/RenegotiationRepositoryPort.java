package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Renegotiation;
import java.util.Optional;

public interface RenegotiationRepositoryPort {
	Renegotiation save(Renegotiation renegotiation);

	/** The renegotiation that replaced the given receivable, if it was renegotiated. */
	Optional<Renegotiation> findByOriginalReceivableId(ReceivableId receivableId);
}
