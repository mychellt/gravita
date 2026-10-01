package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.PosSessionId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NfceRepositoryPort {

	NfceSale save(NfceSale sale);

	Optional<NfceSale> findById(NfceSaleId id);

	Optional<NfceSale> findMostRecent();

	List<NfceSale> findBySessionId(PosSessionId sessionId);

	/**
	 * The NFC-e sales authorized and registered over {@code [from, to)}, oldest first. The sale keeps no
	 * authorization instant of its own, so its registration instant places it in a period.
	 */
	List<NfceSale> findAuthorizedBetween(Instant from, Instant to);
}
