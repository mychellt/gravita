package br.gravita.core.ports.inbound.tax;

import java.util.UUID;

public interface GetInboundNfeForConferenceUseCase {
	InboundNfeConferenceView execute(UUID inboundNfeId);
}
