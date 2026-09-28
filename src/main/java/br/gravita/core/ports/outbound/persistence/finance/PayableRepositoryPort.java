package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import java.util.Optional;

public interface PayableRepositoryPort {
	Payable save(Payable payable);

	Optional<Payable> findById(PayableId id);
}
