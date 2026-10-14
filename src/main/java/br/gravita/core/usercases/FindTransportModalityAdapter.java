package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.FindTransportModalityPort;
import br.gravita.core.ports.outbound.persistence.TransportModalityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FindTransportModalityAdapter implements FindTransportModalityPort {

	private final TransportModalityRepositoryPort transportModalityRepositoryPort;

	public FindTransportModalityAdapter(final TransportModalityRepositoryPort transportModalityRepositoryPort) {
		this.transportModalityRepositoryPort = transportModalityRepositoryPort;
	}

	@Override
	public TransportModalityDomain execute(final Context context) {
		final UUID id = context.getData(UUID.class);
		return transportModalityRepositoryPort.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transport modality not found: " + id));
	}
}
