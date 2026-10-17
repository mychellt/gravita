package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.system.UserId;
import java.util.Objects;
import java.util.UUID;

public record GetInboundNfeConferenceQuery(UUID inboundNfeId, UserId callerId) {

	public GetInboundNfeConferenceQuery {
		Objects.requireNonNull(inboundNfeId, "inboundNfeId is required");
		Objects.requireNonNull(callerId, "callerId is required");
	}
}
