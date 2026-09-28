package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.finance.InternalCashBoxId;
import java.util.Optional;

public interface InternalCashBoxRepositoryPort {

	Optional<InternalCashBox> findById(InternalCashBoxId id);

	/**
	 * Like {@link #findById} but locks the box until the surrounding transaction ends,
	 * so concurrent movements update the balance one at a time.
	 */
	Optional<InternalCashBox> findByIdForUpdate(InternalCashBoxId id);

	InternalCashBox save(InternalCashBox cashBox);

	CashMovement saveMovement(CashMovement movement);
}
