package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseNumber;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistence of the {@link NfseDocument} aggregate, including its pre-conversion RPS state. */
public interface NfseRepositoryPort {

	NfseDocument save(NfseDocument document);

	Optional<NfseDocument> findById(NfseId id);

	/**
	 * Loads the document holding a write lock until the surrounding transaction ends, so two concurrent conversions
	 * of the same RPS are serialized. Must be called inside a transaction.
	 */
	Optional<NfseDocument> findByIdForUpdate(NfseId id);

	/**
	 * Allocates the next NFSe number of the {@code (company, municipality)} sequence, which is independent of the
	 * NFe/NFCe/RPS series. Must be called inside a transaction: the number is only consumed if it commits.
	 */
	NfseNumber allocateNextNumber(CompanyId companyId, String municipalityIbgeCode);

	/** The NFSe authorized over {@code [from, to)}, oldest authorization first. Cancelled ones are not included. */
	List<NfseDocument> findAuthorizedBetween(Instant from, Instant to);
}
