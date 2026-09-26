package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.PosSessionId;

public interface OpenPosSessionUseCase {
	PosSessionId execute(OpenPosSessionCommand command);
}
