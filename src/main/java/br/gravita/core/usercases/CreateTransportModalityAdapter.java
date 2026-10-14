package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.ports.business.CreateTransportModalityPort;
import br.gravita.core.ports.outbound.persistence.TransportModalityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateTransportModalityAdapter implements CreateTransportModalityPort {

	private final TransportModalityRepositoryPort transportModalityRepositoryPort;

	public CreateTransportModalityAdapter(final TransportModalityRepositoryPort transportModalityRepositoryPort) {
		this.transportModalityRepositoryPort = transportModalityRepositoryPort;
	}

	@Override
	public TransportModalityDomain execute(final Context context) {
		final TransportModalityDomain modality = context.getData(TransportModalityDomain.class);
		if (modality.getName() == null || modality.getName().isBlank()) {
			throw new BusinessRuleException("Transport modality name is required");
		}
		modality.setName(modality.getName().trim());
		modality.setCode(TransportModalityDomain.normalizeCode(modality.getCode()));
		modality.setActive(modality.isEnabled());
		if (modality.isEnabled()) {
			transportModalityRepositoryPort.findActiveByCode(modality.getCode()).ifPresent(existing -> {
				throw new DuplicateResourceException("An active transport modality with code " + modality.getCode() + " already exists");
			});
		}
		if (modality.getId() == null) {
			modality.setId(UUID.randomUUID());
		}
		return transportModalityRepositoryPort.save(modality);
	}
}
