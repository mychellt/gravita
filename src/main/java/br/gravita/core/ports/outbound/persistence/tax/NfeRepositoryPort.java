package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import java.util.Optional;

public interface NfeRepositoryPort {

	NfeDocument save(NfeDocument document);

	Optional<NfeDocument> findById(NfeDocumentId id);
}
