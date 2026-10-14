package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.TransportModalityDomain;

import java.util.List;

/** Context data: a {@code Boolean} includeInactive (null or false lists only the active modalities). */
public interface ListTransportModalitiesPort extends Command<List<TransportModalityDomain>> {
}
