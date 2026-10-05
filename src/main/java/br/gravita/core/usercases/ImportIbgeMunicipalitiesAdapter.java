package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.ports.business.ImportIbgeMunicipalitiesPort;
import br.gravita.core.ports.outbound.persistence.IbgeMunicipalityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ImportIbgeMunicipalitiesAdapter implements ImportIbgeMunicipalitiesPort {

	private final IbgeMunicipalityRepositoryPort ibgeMunicipalityRepositoryPort;

	public ImportIbgeMunicipalitiesAdapter(final IbgeMunicipalityRepositoryPort ibgeMunicipalityRepositoryPort) {
		this.ibgeMunicipalityRepositoryPort = ibgeMunicipalityRepositoryPort;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IbgeMunicipalityDomain> execute(final Context context) {
		final List<IbgeMunicipalityDomain> incoming = (List<IbgeMunicipalityDomain>) context.getData(List.class);
		incoming.forEach(municipality -> ibgeMunicipalityRepositoryPort.findByIbgeCode(municipality.getIbgeCode())
				.ifPresentOrElse(
						existing -> municipality.setId(existing.getId()),
						() -> municipality.setId(UUID.randomUUID())));
		return ibgeMunicipalityRepositoryPort.saveAll(incoming);
	}
}
