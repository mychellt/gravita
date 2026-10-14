package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.DeleteTransportModalityPort;
import br.gravita.core.ports.outbound.persistence.TransportModalityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Soft delete: the row is only flagged inactive. NF-e documents store the resolved modality (not a reference to
 * the registry row), so an issued document is never touched by this operation.
 */
@Component
public class DeleteTransportModalityAdapter implements DeleteTransportModalityPort {

	private final TransportModalityRepositoryPort transportModalityRepositoryPort;

	public DeleteTransportModalityAdapter(final TransportModalityRepositoryPort transportModalityRepositoryPort) {
		this.transportModalityRepositoryPort = transportModalityRepositoryPort;
	}

	@Override
	public Void execute(final Context context) {
		final UUID id = context.getData(UUID.class);
		final TransportModalityDomain modality = transportModalityRepositoryPort.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transport modality not found: " + id));
		if (modality.isEnabled()) {
			modality.setActive(false);
			transportModalityRepositoryPort.save(modality);
		}
		return null;
	}
}
