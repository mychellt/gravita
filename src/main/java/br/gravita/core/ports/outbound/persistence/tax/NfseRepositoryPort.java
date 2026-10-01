package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import java.util.Optional;

/** Persistence of the {@link NfseDocument} aggregate, including its pre-conversion RPS state. */
public interface NfseRepositoryPort {

	NfseDocument save(NfseDocument document);

	Optional<NfseDocument> findById(NfseId id);
}
