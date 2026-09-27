package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.NfeDocument;

public interface NfeRepositoryPort {
	NfeDocument save(NfeDocument nfeDocument);
}
