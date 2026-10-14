package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.ports.business.ListTransportModalitiesPort;
import br.gravita.core.ports.outbound.persistence.TransportModalityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListTransportModalitiesAdapter implements ListTransportModalitiesPort {

	private final TransportModalityRepositoryPort transportModalityRepositoryPort;

	public ListTransportModalitiesAdapter(final TransportModalityRepositoryPort transportModalityRepositoryPort) {
		this.transportModalityRepositoryPort = transportModalityRepositoryPort;
	}

	@Override
	public List<TransportModalityDomain> execute(final Context context) {
		final boolean includeInactive = Boolean.TRUE.equals(context.getData(Boolean.class));
		return includeInactive ? transportModalityRepositoryPort.findAll() : transportModalityRepositoryPort.findAllActive();
	}
}
