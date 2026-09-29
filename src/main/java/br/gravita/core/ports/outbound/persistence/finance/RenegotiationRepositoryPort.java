package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Renegotiation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RenegotiationRepositoryPort {
	Renegotiation save(Renegotiation renegotiation);

	/** The renegotiation that replaced the given receivable, if it was renegotiated. */
	Optional<Renegotiation> findByOriginalReceivableId(ReceivableId receivableId);

	/** The renegotiations agreed with the customer, oldest first. */
	List<Renegotiation> findByCustomerId(UUID customerId);
}
