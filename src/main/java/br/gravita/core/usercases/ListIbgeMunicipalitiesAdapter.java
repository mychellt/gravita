package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.ports.business.ListIbgeMunicipalitiesPort;
import br.gravita.core.ports.persistence.IbgeMunicipalityRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListIbgeMunicipalitiesAdapter implements ListIbgeMunicipalitiesPort {

	private final IbgeMunicipalityRepositoryPort ibgeMunicipalityRepositoryPort;

	public ListIbgeMunicipalitiesAdapter(IbgeMunicipalityRepositoryPort ibgeMunicipalityRepositoryPort) {
		this.ibgeMunicipalityRepositoryPort = ibgeMunicipalityRepositoryPort;
	}

	@Override
	public List<IbgeMunicipalityDomain> execute(Context context) {
		return ibgeMunicipalityRepositoryPort.findAll();
	}
}
