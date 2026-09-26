package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;

import java.util.Optional;
import java.util.UUID;

public interface PosSessionRepositoryPort {

	PosSession save(PosSession posSession);

	Optional<PosSession> findById(PosSessionId id);

	boolean existsByRegisterIdAndStatus(UUID registerId, PosSessionStatus status);
}
