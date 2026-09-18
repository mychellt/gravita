package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.IbgeMunicipalityDomain;

import java.util.List;

public interface ImportIbgeMunicipalitiesPort extends Command<List<IbgeMunicipalityDomain>> {
}
