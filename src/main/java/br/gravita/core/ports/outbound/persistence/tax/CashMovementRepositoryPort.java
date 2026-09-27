package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.CashMovement;
import br.gravita.core.domain.tax.PosSessionId;
import java.util.List;

public interface CashMovementRepositoryPort {

	CashMovement save(CashMovement cashMovement);

	List<CashMovement> findBySessionId(PosSessionId sessionId);
}
