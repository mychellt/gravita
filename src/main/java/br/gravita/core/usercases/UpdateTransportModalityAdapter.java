package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.UpdateTransportModalityPort;
import br.gravita.core.ports.outbound.persistence.TransportModalityRepositoryPort;
import org.springframework.stereotype.Component;

/** Context data: the requested changes; a null {@code active} keeps the stored flag. */
@Component
public class UpdateTransportModalityAdapter implements UpdateTransportModalityPort {

	private final TransportModalityRepositoryPort transportModalityRepositoryPort;

	public UpdateTransportModalityAdapter(final TransportModalityRepositoryPort transportModalityRepositoryPort) {
		this.transportModalityRepositoryPort = transportModalityRepositoryPort;
	}

	@Override
	public TransportModalityDomain execute(final Context context) {
		final TransportModalityDomain changes = context.getData(TransportModalityDomain.class);
		final TransportModalityDomain modality = transportModalityRepositoryPort.get(changes.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Transport modality not found: " + changes.getId()));
		if (changes.getName() == null || changes.getName().isBlank()) {
			throw new BusinessRuleException("Transport modality name is required");
		}
		modality.setName(changes.getName().trim());
		modality.setCode(TransportModalityDomain.normalizeCode(changes.getCode()));
		if (changes.getActive() != null) {
			modality.setActive(changes.getActive());
		}
		if (modality.isEnabled()) {
			transportModalityRepositoryPort.findActiveByCode(modality.getCode())
					.filter(other -> !other.getId().equals(modality.getId()))
					.ifPresent(other -> {
						throw new DuplicateResourceException("An active transport modality with code " + modality.getCode() + " already exists");
					});
		}
		return transportModalityRepositoryPort.save(modality);
	}
}
