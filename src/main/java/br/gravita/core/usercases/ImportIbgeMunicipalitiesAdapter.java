package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.ports.business.ImportIbgeMunicipalitiesPort;
import br.gravita.core.ports.persistence.IbgeMunicipalityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Bulk import is the only write path for this table (AC: pre-loaded reference data, not manual entry).
 * Rows are upserted by their natural key (ibgeCode) so re-running an import is idempotent.
 */
@Component
public class ImportIbgeMunicipalitiesAdapter implements ImportIbgeMunicipalitiesPort {

	private final IbgeMunicipalityRepositoryPort ibgeMunicipalityRepositoryPort;

	public ImportIbgeMunicipalitiesAdapter(IbgeMunicipalityRepositoryPort ibgeMunicipalityRepositoryPort) {
		this.ibgeMunicipalityRepositoryPort = ibgeMunicipalityRepositoryPort;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IbgeMunicipalityDomain> execute(Context context) {
		List<IbgeMunicipalityDomain> incoming = (List<IbgeMunicipalityDomain>) context.getData(List.class);
		incoming.forEach(municipality -> ibgeMunicipalityRepositoryPort.findByIbgeCode(municipality.getIbgeCode())
				.ifPresentOrElse(
						existing -> municipality.setId(existing.getId()),
						() -> municipality.setId(UUID.randomUUID())));
		return ibgeMunicipalityRepositoryPort.saveAll(incoming);
	}
}
