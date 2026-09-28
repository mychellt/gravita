package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeId;
import br.gravita.core.domain.finance.ReceivableId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PixChargeRepositoryPort {
	PixCharge save(PixCharge pixCharge);

	Optional<PixCharge> findById(PixChargeId id);

	List<PixCharge> findByReceivableId(ReceivableId receivableId);

	/** The {@code PENDING} charges whose {@code expiresAt} is before {@code now}. */
	List<PixCharge> findPendingExpiredBefore(Instant now);
}
